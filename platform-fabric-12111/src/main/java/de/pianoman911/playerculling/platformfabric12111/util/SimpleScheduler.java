package de.pianoman911.playerculling.platformfabric12111.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleScheduler {

    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private final Map<Integer, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger currentId = new AtomicInteger();

    public int scheduleRepeating(Runnable runnable, long delay, long period) {
        ScheduledFuture<?> scheduledFuture = this.scheduler.scheduleAtFixedRate(runnable, delay, period, TimeUnit.MILLISECONDS);
        int id = this.currentId.getAndIncrement();
        this.tasks.put(id, scheduledFuture);
        return id;
    }

    public int scheduleDelayed(Runnable runnable, long delay) {
        ScheduledFuture<?> scheduledFuture = this.scheduler.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        int id = this.currentId.getAndIncrement();
        this.tasks.put(id, scheduledFuture);
        return id;
    }

    public void cancel(int id) {
        ScheduledFuture<?> scheduledFuture = this.tasks.remove(id);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    public void shutdown() throws InterruptedException {
        this.scheduler.shutdown();
        this.scheduler.awaitTermination(3L, TimeUnit.SECONDS);
    }
}
