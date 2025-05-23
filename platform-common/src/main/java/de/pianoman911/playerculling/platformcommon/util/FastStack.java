package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

@NullMarked
public final class FastStack<T> {

    private static final int GROW_RATE = 3;

    private int capacity;
    private int index;
    private T[] stack;

    @SuppressWarnings("unchecked")
    public FastStack(int initialCapacity) {
        this.capacity = initialCapacity;
        this.stack = (T[]) new Object[initialCapacity];
    }

    public void push(T o) {
        this.stack[this.index++] = o;
    }

    @Nullable
    public T pop() {
        if (this.index == 0) {
            return null;
        }
        return this.stack[--this.index];
    }

    public boolean isEmpty() {
        return this.index == 0;
    }

    public boolean hasEntries() {
        return this.index > 0;
    }

    public void grow(int targetIndex) {
        if (this.capacity > targetIndex) {
            return;
        }
        this.capacity = targetIndex + GROW_RATE;
        this.stack = Arrays.copyOf(this.stack, this.capacity);
    }
}
