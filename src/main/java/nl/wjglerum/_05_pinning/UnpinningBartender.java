package nl.wjglerum._05_pinning;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.wjglerum.client.CoffeeMachineClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class UnpinningBartender {

    @Inject
    @RestClient
    CoffeeMachineClient coffeeMachine;

    // ReentrantLock does NOT pin — virtual thread yields to its carrier during the HTTP wait
    private final ReentrantLock lock = new ReentrantLock();

    public UnpinningBeverage get() {
        Log.info("Warming up the unpinning coffee machine (NOT pinned)");
        lock.lock();
        try {
            var response = coffeeMachine.brew();
            return new UnpinningBeverage("Unpinning " + response.name());
        } finally {
            lock.unlock();
        }
    }
}
