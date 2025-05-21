package de.pianoman911.playerculling.platformcommon.util;

import it.unimi.dsi.fastutil.bytes.Byte2IntFunction;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectFunction;
import it.unimi.dsi.fastutil.chars.Char2IntFunction;
import it.unimi.dsi.fastutil.chars.Char2ObjectFunction;
import it.unimi.dsi.fastutil.doubles.Double2IntFunction;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import it.unimi.dsi.fastutil.floats.Float2IntFunction;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ByteFunction;
import it.unimi.dsi.fastutil.ints.Int2CharFunction;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import it.unimi.dsi.fastutil.ints.Int2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2LongFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceFunction;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ByteFunction;
import it.unimi.dsi.fastutil.objects.Object2CharFunction;
import it.unimi.dsi.fastutil.objects.Object2DoubleFunction;
import it.unimi.dsi.fastutil.objects.Object2FloatFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2LongFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ReferenceFunction;
import it.unimi.dsi.fastutil.objects.Object2ShortFunction;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntFunction;
import it.unimi.dsi.fastutil.objects.Reference2ObjectFunction;
import it.unimi.dsi.fastutil.shorts.Short2IntFunction;
import it.unimi.dsi.fastutil.shorts.Short2ObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullUnmarked;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings("deprecation")
@NullUnmarked
public class ForwardedInt2ObjectMap<T> implements Int2ObjectMap<T> {

    private final Int2ObjectMap<T> delegate;

    public ForwardedInt2ObjectMap(Int2ObjectMap<T> delegate) {
        this.delegate = delegate;
    }

    public Int2ObjectMap<T> getDelegate() {
        return delegate;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public void defaultReturnValue(T rv) {
        this.delegate.defaultReturnValue(rv);
    }

    @Override
    public T defaultReturnValue() {
        return this.delegate.defaultReturnValue();
    }

    @Override
    public ObjectSet<Entry<T>> int2ObjectEntrySet() {
        return this.delegate.int2ObjectEntrySet();
    }

    @Deprecated
    @Override
    public @NotNull ObjectSet<Map.Entry<Integer, T>> entrySet() {
        return this.delegate.entrySet();
    }

    @Deprecated
    @Override
    public T put(Integer key, T value) {
        return this.delegate.put(key, value);
    }

    @Deprecated
    @Override
    public T get(Object key) {
        return this.delegate.get(key);
    }

    @Deprecated
    @Override
    public T remove(Object key) {
        return this.delegate.remove(key);
    }

    @Override
    public @NotNull IntSet keySet() {
        return this.delegate.keySet();
    }

    @Override
    public @NotNull ObjectCollection<T> values() {
        return this.delegate.values();
    }

    @Override
    public boolean containsKey(int key) {
        return this.delegate.containsKey(key);
    }

    @Deprecated
    @Override
    public boolean containsKey(Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public void forEach(BiConsumer<? super Integer, ? super T> consumer) {
        this.delegate.forEach(consumer);
    }

    @Override
    public T getOrDefault(int key, T defaultValue) {
        return this.delegate.getOrDefault(key, defaultValue);
    }

    @Deprecated
    @Override
    public T getOrDefault(Object key, T defaultValue) {
        return this.delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public T putIfAbsent(int key, T value) {
        return this.delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(int key, Object value) {
        return this.delegate.remove(key, value);
    }

    @Override
    public boolean replace(int key, T oldValue, T newValue) {
        return this.delegate.replace(key, oldValue, newValue);
    }

    @Override
    public T replace(int key, T value) {
        return this.delegate.replace(key, value);
    }

    @Override
    public T computeIfAbsent(int key, IntFunction<? extends T> mappingFunction) {
        return this.delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public T computeIfAbsent(int key, Int2ObjectFunction<? extends T> mappingFunction) {
        return this.delegate.computeIfAbsent(key, mappingFunction);
    }

    @Deprecated
    @Override
    public T computeIfAbsentPartial(int key, Int2ObjectFunction<? extends T> mappingFunction) {
        return this.delegate.computeIfAbsentPartial(key, mappingFunction);
    }

    @Override
    public T computeIfPresent(int key, BiFunction<? super Integer, ? super T, ? extends T> remappingFunction) {
        return this.delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public T compute(int key, BiFunction<? super Integer, ? super T, ? extends T> remappingFunction) {
        return this.delegate.compute(key, remappingFunction);
    }

    @Override
    public T merge(int key, T value, BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
        return this.delegate.merge(key, value, remappingFunction);
    }

    @Override
    public T apply(int operand) {
        return this.delegate.apply(operand);
    }

    @Override
    public T put(int key, T value) {
        return this.delegate.put(key, value);
    }

    @Override
    public T get(int key) {
        return this.delegate.get(key);
    }

    @Override
    public T remove(int key) {
        return this.delegate.remove(key);
    }

    @Deprecated
    @Override
    public <T1> @NotNull Function<T1, T> compose(Function<? super T1, ? extends Integer> before) {
        return this.delegate.compose(before);
    }

    @Override
    public Int2ByteFunction andThenByte(Object2ByteFunction<T> after) {
        return this.delegate.andThenByte(after);
    }

    @Override
    public Byte2ObjectFunction<T> composeByte(Byte2IntFunction before) {
        return this.delegate.composeByte(before);
    }

    @Override
    public Int2ShortFunction andThenShort(Object2ShortFunction<T> after) {
        return this.delegate.andThenShort(after);
    }

    @Override
    public Short2ObjectFunction<T> composeShort(Short2IntFunction before) {
        return this.delegate.composeShort(before);
    }

    @Override
    public Int2IntFunction andThenInt(Object2IntFunction<T> after) {
        return this.delegate.andThenInt(after);
    }

    @Override
    public Int2ObjectFunction<T> composeInt(Int2IntFunction before) {
        return this.delegate.composeInt(before);
    }

    @Override
    public Int2LongFunction andThenLong(Object2LongFunction<T> after) {
        return this.delegate.andThenLong(after);
    }

    @Override
    public Long2ObjectFunction<T> composeLong(Long2IntFunction before) {
        return this.delegate.composeLong(before);
    }

    @Override
    public Int2CharFunction andThenChar(Object2CharFunction<T> after) {
        return this.delegate.andThenChar(after);
    }

    @Override
    public Char2ObjectFunction<T> composeChar(Char2IntFunction before) {
        return this.delegate.composeChar(before);
    }

    @Override
    public Int2FloatFunction andThenFloat(Object2FloatFunction<T> after) {
        return this.delegate.andThenFloat(after);
    }

    @Override
    public Float2ObjectFunction<T> composeFloat(Float2IntFunction before) {
        return this.delegate.composeFloat(before);
    }

    @Override
    public Int2DoubleFunction andThenDouble(Object2DoubleFunction<T> after) {
        return this.delegate.andThenDouble(after);
    }

    @Override
    public Double2ObjectFunction<T> composeDouble(Double2IntFunction before) {
        return this.delegate.composeDouble(before);
    }

    @Override
    public <T1> Int2ObjectFunction<T1> andThenObject(Object2ObjectFunction<? super T, ? extends T1> after) {
        return this.delegate.andThenObject(after);
    }

    @Override
    public <T1> Object2ObjectFunction<T1, T> composeObject(Object2IntFunction<? super T1> before) {
        return this.delegate.composeObject(before);
    }

    @Override
    public <T1> Int2ReferenceFunction<T1> andThenReference(Object2ReferenceFunction<? super T, ? extends T1> after) {
        return this.delegate.andThenReference(after);
    }

    @Override
    public <T1> Reference2ObjectFunction<T1, T> composeReference(Reference2IntFunction<? super T1> before) {
        return this.delegate.composeReference(before);
    }

    @Override
    public T apply(Integer key) {
        return this.delegate.apply(key);
    }

    @Override
    public @NotNull <V> Function<Integer, V> andThen(@NotNull Function<? super T, ? extends V> after) {
        return this.delegate.andThen(after);
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public void putAll(@NotNull Map<? extends Integer, ? extends T> m) {
        this.delegate.putAll(m);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return this.delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public void replaceAll(BiFunction<? super Integer, ? super T, ? extends T> function) {
        this.delegate.replaceAll(function);
    }

    @Override
    public @Nullable T putIfAbsent(Integer key, T value) {
        return this.delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.delegate.remove(key, value);
    }

    @Override
    public boolean replace(Integer key, T oldValue, T newValue) {
        return this.delegate.replace(key, oldValue, newValue);
    }

    @Override
    public @Nullable T replace(Integer key, T value) {
        return this.delegate.replace(key, value);
    }

    @Override
    public T computeIfAbsent(Integer key, @NotNull Function<? super Integer, ? extends T> mappingFunction) {
        return this.delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public T computeIfPresent(Integer key, @NotNull BiFunction<? super Integer, ? super T, ? extends T> remappingFunction) {
        return this.delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public T compute(Integer key, @NotNull BiFunction<? super Integer, ? super @Nullable T, ? extends T> remappingFunction) {
        return this.delegate.compute(key, remappingFunction);
    }

    @Override
    public T merge(Integer key, @NotNull T value, @NotNull BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
        return this.delegate.merge(key, value, remappingFunction);
    }
}
