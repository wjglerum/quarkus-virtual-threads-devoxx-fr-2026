package nl.wjglerum._05_pinning;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.wjglerum.client.CoffeeMachineClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PinningBartender {

    @Inject
    @RestClient
    CoffeeMachineClient coffeeMachine;

    // synchronized pins the virtual thread to its carrier for the duration of the HTTP call
    public synchronized PinningBeverage get() {
        Log.info("Warming up the pinning coffee machine (PINNED!)");
        var response = coffeeMachine.brew();
        return new PinningBeverage("Pinning " + response.name());
    }
}
