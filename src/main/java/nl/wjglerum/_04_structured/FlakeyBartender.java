package nl.wjglerum._04_structured;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class FlakeyBartender {

    @ConfigProperty(name = "bartender.delay")
    Duration delay;

    public StructuredBeverage get() {
        Log.info("Warming up the flakey coffee machine (50% chance of failure)");
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (ThreadLocalRandom.current().nextBoolean()) {
            throw new RuntimeException("Coffee machine broke!");
        }
        return new StructuredBeverage("Flakey coffee");
    }
}
