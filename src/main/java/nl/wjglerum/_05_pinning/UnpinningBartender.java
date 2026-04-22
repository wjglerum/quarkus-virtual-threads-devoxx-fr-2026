package nl.wjglerum._05_pinning;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class UnpinningBartender {

    @ConfigProperty(name = "bartender.delay")
    Duration delay;

    // ReentrantLock does NOT pin — virtual thread yields to its carrier during park
    private final ReentrantLock lock = new ReentrantLock();

    public UnpinningBeverage get() {
        Log.info("Warming up the unpinning coffee machine (NOT pinned)");
        lock.lock();
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return new UnpinningBeverage("Unpinning coffee");
    }
}
