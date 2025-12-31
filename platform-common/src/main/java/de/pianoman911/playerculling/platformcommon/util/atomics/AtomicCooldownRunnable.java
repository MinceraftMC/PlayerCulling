package de.pianoman911.playerculling.platformcommon.util.atomics;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicCooldownRunnable {

    private final AtomicLong lastExecution = new AtomicLong(0);
    private final long cooldownMillis;

    public AtomicCooldownRunnable(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public void run(Runnable runnable) {
        long timestamp = this.lastExecution.getAndUpdate(current -> { // Ensure atomic check-and-set
            long now = System.currentTimeMillis();
            if (now - current >= this.cooldownMillis) {
                return now;
            }
            return current;
        });
        if (System.currentTimeMillis() - timestamp < this.cooldownMillis) {
            return;
        }
        runnable.run();
    }
}
