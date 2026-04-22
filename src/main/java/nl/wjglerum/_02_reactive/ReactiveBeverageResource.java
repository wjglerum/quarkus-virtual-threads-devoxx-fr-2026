package nl.wjglerum._02_reactive;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import nl.wjglerum.FloodResult;

import java.util.List;
import java.util.stream.IntStream;

@Path("/beverage/reactive")
public class ReactiveBeverageResource {

    @Inject
    ReactiveBartender bartender;

    @Inject
    ReactiveBeverageRepository repository;

    @GET
    @WithTransaction
    public Uni<ReactiveBeverage> getBeverage() {
        Log.info("Going to get reactive beverage");
        return bartender.get().onItem().call(beverage -> repository.save(beverage));
    }

    @GET
    @Path("/sequential")
    @WithTransaction
    public Uni<List<ReactiveBeverage>> getBeverageSequential() {
        Log.info("Going to get reactive beverages sequential");
        return bartender.get().onItem().transformToUni(beverage1 ->
                bartender.get().onItem().transformToUni(beverage2 ->
                        bartender.get().onItem().transformToUni(beverage3 -> {
                                    var beverages = List.of(beverage1, beverage2, beverage3);
                                    return repository.save(beverages).replaceWith(beverages);
                                }
                        )
                )
        );
    }

    @GET
    @Path("/parallel")
    @WithTransaction
    public Uni<List<ReactiveBeverage>> getBeveragesParallel() {
        Log.info("Going to get reactive beverages parallel");
        var beverage1 = bartender.get();
        var beverage2 = bartender.get();
        var beverage3 = bartender.get();
        return Uni.join().all(beverage1, beverage2, beverage3).andCollectFailures()
                .onItem().call(beverages -> repository.save(beverages));
    }

    @GET
    @Path("/flood")
    public Uni<FloodResult> flood(@QueryParam("count") @DefaultValue("100") int count) {
        Log.infof("Flooding with %d reactive requests", count);
        var start = System.currentTimeMillis();
        var unis = IntStream.range(0, count)
                .mapToObj(i -> bartender.get().map(b -> 1).onFailure().recoverWithItem(0))
                .toList();
        return Uni.join().all(unis).andCollectFailures()
                .map(results -> {
                    var succeeded = results.stream().mapToInt(Integer::intValue).sum();
                    return new FloodResult(count, succeeded, count - succeeded, System.currentTimeMillis() - start);
                });
    }
}
