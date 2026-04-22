package nl.wjglerum._04_structured;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Path("/beverage/structured")
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
    @Transactional
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
    @Transactional
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
     * Race mode: fork 3 bartenders, persist the first one to finish.
     * The other two are cancelled automatically when the scope closes.
     */
    @GET
    @Path("/race")
    @Transactional
    public StructuredBeverage getBeverageRace() throws InterruptedException {
        Log.info("Going to race 3 bartenders — first one wins");
        var joiner = StructuredTaskScope.Joiner.<StructuredBeverage>anySuccessfulOrThrow();
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            scope.fork(bartender::get);
            var winner = scope.join();
            repository.save(winner);
            return winner;
        }
    }

    /**
     * Fail-fast: if any subtask throws, the scope cancels the remaining ones immediately.
     * Shows StructuredTaskScope.Joiner.allSuccessfulOrThrow() in action with a flakey bartender.
     */
    @GET
    @Path("/failfast")
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
     * Timeout: the whole scope is cancelled after 150 ms — always fires in dev (3 s delay),
     * may or may not fire in test (100 ms delay). Subtasks are cancelled automatically.
     */
    @GET
    @Path("/timeout")
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
