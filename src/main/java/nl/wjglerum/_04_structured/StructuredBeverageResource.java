package nl.wjglerum._04_structured;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Path("/beverage/structured")
@Transactional
@RunOnVirtualThread
@SuppressWarnings("preview")
public class StructuredBeverageResource {

    @Inject
    StructuredBartender bartender;

    @Inject
    FlakeyBartender flakeyBartender;

    @Inject
    StructuredBeverageRepository repository;

    @GET
    @Path("/simple")
    public List<StructuredBeverage> getBeveragesSimple() throws InterruptedException {
        Log.info("Going to get structured beverages simple");
        try (var scope = StructuredTaskScope.open()) {
            var beverage1 = scope.fork(bartender::get);
            var beverage2 = scope.fork(bartender::get);
            var beverage3 = scope.fork(bartender::get);
            scope.join();
            var beverages = List.of(beverage1.get(), beverage2.get(), beverage3.get());
            repository.save(beverages);
            return beverages;
        }
    }

    @GET
    @Path("/custom")
    public List<StructuredBeverage> getBeveragesCustom() throws InterruptedException {
        Log.info("Going to get structured beverages custom");
        var joiner = StructuredTaskScope.Joiner.<StructuredBeverage>allSuccessfulOrThrow();
        var currentThread = Thread.currentThread().getName();
        var threadFactory = Thread.ofVirtual().name(currentThread + "-structured-beverage-", 0).factory();
        try (var scope = StructuredTaskScope.open(joiner, cf -> cf.withThreadFactory(threadFactory))) {
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            var beverages = scope.join();
            repository.save(beverages);
            return beverages;
        }
    }

    /**
     * Race mode: fork 3 bartenders, return the first one to finish.
     * The other two are cancelled automatically when the scope closes.
     */
    @GET
    @Path("/race")
    public StructuredBeverage getBeverageRace() throws InterruptedException {
        Log.info("Going to race 3 bartenders — first one wins");
        var joiner = StructuredTaskScope.Joiner.<StructuredBeverage>anySuccessfulOrThrow();
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            return scope.join();
        }
    }

    /**
     * Fail-fast: if any subtask throws, the scope cancels the remaining ones immediately.
     * Shows StructuredTaskScope.Joiner.allSuccessfulOrThrow() in action with a flakey bartender.
     */
    @GET
    @Path("/failfast")
    @Transactional(jakarta.transaction.Transactional.TxType.NOT_SUPPORTED)
    public Response getBeveragesFailFast() throws InterruptedException {
        Log.info("Going to get beverages fail-fast — one failure cancels siblings");
        var joiner = StructuredTaskScope.Joiner.<StructuredBeverage>allSuccessfulOrThrow();
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(flakeyBartender::get);
            scope.fork(flakeyBartender::get);
            scope.fork(flakeyBartender::get);
            try {
                var beverages = scope.join();
                return Response.ok(beverages).build();
            } catch (StructuredTaskScope.FailedException e) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\":\"" + e.getCause().getMessage() + "\"}")
                        .build();
            }
        }
    }

    /**
     * Timeout: the whole scope is cancelled if no result arrives within 2x the bartender delay.
     */
    @GET
    @Path("/timeout")
    @Transactional(jakarta.transaction.Transactional.TxType.NOT_SUPPORTED)
    public Response getBeveragesWithTimeout() throws InterruptedException {
        Log.info("Going to get beverages with scope-level timeout");
        var joiner = StructuredTaskScope.Joiner.<StructuredBeverage>allSuccessfulOrThrow();
        try (var scope = StructuredTaskScope.open(joiner, cf -> cf.withTimeout(Duration.ofMillis(150)))) {
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            try {
                var beverages = scope.join();
                return Response.ok(beverages).build();
            } catch (StructuredTaskScope.TimeoutException e) {
                return Response.status(Response.Status.REQUEST_TIMEOUT)
                        .entity("{\"error\":\"scope timed out — all subtasks cancelled\"}")
                        .build();
            } catch (StructuredTaskScope.FailedException e) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\":\"" + e.getCause().getMessage() + "\"}")
                        .build();
            }
        }
    }
}
