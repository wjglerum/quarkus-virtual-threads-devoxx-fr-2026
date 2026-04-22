package nl.wjglerum._05_pinning;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class PinningBartender {

    @ConfigProperty(name = "bartender.delay")
    Duration delay;

    // synchronized pins the virtual thread to its carrier — this is the gotcha
    public synchronized PinningBeverage get() {
        Log.info("Warming up the pinning coffee machine (PINNED!)");
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new PinningBeverage("Pinning coffee");
    }
}
