package de.pianoman911.playerculling.platformcommon.util;
// Created by booky10 in PlayerCulling (21:18 21.05.2025)

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public class TickRefreshSupplier<T> implements Supplier<T> {

    private final IPlatform platform;
    private final Supplier<T> delegate;
    private volatile long lastTick;
    private volatile @MonotonicNonNull T value;

    public TickRefreshSupplier(IPlatform platform, Supplier<T> delegate) {
        this.platform = platform;
        this.delegate = delegate;
    }

    @Override
    public T get() {
        long currentTick = this.platform.getCurrentTick();
        if (this.lastTick != currentTick) {
            synchronized (this) {
                if (this.lastTick != currentTick) {
                    T value = this.delegate.get();
                    this.value = value;
                    this.lastTick = currentTick;
                    return value;
                }
            }
        }
        return this.value;
    }
}
