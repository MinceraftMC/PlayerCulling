package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@NullMarked
public final class AtomicFastStack<T> {

    private static final int GROW_RATE = 3;
    private final AtomicInteger index = new AtomicInteger(-1);
    private int capacity;
    private T[] stack;

    @SuppressWarnings("unchecked")
    public AtomicFastStack(int initialCapacity) {
        this.capacity = initialCapacity;
        this.stack = (T[]) new Object[initialCapacity];
    }

    public void push(T o) {
        this.stack[this.index.incrementAndGet()] = o;
    }

    @Nullable
    public T pop() {
        int index = this.index.getAndUpdate(i -> Math.max(-1, i - 1));
        if (index < 0) {
            return null;
        }
        return this.stack[index];
    }

    public boolean isEmpty() {
        return this.index.get() < 0;
    }

    public boolean hasEntries() {
        return this.index.get() >= 0;
    }

    public int size() {
        return this.index.get() + 1;
    }

    public void grow(int targetIndex) {
        if (this.capacity > targetIndex) {
            return;
        }
        this.capacity = targetIndex + GROW_RATE;
        this.stack = Arrays.copyOf(this.stack, this.capacity);
    }
}
