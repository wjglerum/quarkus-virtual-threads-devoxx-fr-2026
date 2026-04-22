package nl.wjglerum._05_pinning;

import io.quarkus.logging.Log;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Path("/beverage/pinning")
@RunOnVirtualThread
public class PinningBeverageResource {

    @Inject
    PinningBartender pinningBartender;

    @Inject
    UnpinningBartender unpinningBartender;

    @Inject
    @VirtualThreads
    ExecutorService executor;

    @GET
    @Path("/pinned")
    public PinningBeverage getPinned() {
        Log.info("Getting pinned beverage — virtual thread will be pinned to carrier");
        return pinningBartender.get();
    }

    @GET
    @Path("/unpinned")
    public UnpinningBeverage getUnpinned() {
        Log.info("Getting unpinned beverage — virtual thread yields to carrier");
        return unpinningBartender.get();
    }

    @GET
    @Path("/pinned/parallel")
    public java.util.List<PinningBeverage> getPinnedParallel() throws ExecutionException, InterruptedException {
        Log.info("Getting 3 pinned beverages in parallel — effective concurrency limited by carrier threads");
        var b1 = executor.submit(pinningBartender::get);
        var b2 = executor.submit(pinningBartender::get);
        var b3 = executor.submit(pinningBartender::get);
        return java.util.List.of(b1.get(), b2.get(), b3.get());
    }

    @GET
    @Path("/unpinned/parallel")
    public java.util.List<UnpinningBeverage> getUnpinnedParallel() throws ExecutionException, InterruptedException {
        Log.info("Getting 3 unpinned beverages in parallel — true concurrency on virtual threads");
        var b1 = executor.submit(unpinningBartender::get);
        var b2 = executor.submit(unpinningBartender::get);
        var b3 = executor.submit(unpinningBartender::get);
        return java.util.List.of(b1.get(), b2.get(), b3.get());
    }
}
