package net.vibmc.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public ScheduledFuture<?> scheduleRepeating(Runnable runnable, long initialDelayMs, long periodMs) {
        return executor.scheduleAtFixedRate(runnable, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
