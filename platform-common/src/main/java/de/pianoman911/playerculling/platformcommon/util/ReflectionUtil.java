package de.pianoman911.playerculling.platformcommon.util;
// Created by booky10 in SimplePacketApi (22:27 15.05.23)

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

@SuppressWarnings("deprecation")
public final class ReflectionUtil {

    private static final Unsafe THE_UNSAFE;
    private static final MethodHandles.Lookup TRUSTED_LOOKUP;

    static {
        // UNSAFE + TRUSTED_LOOKUP lookup taken from https://github.com/Lenni0451/Reflect/blob/99ec175e150fd575bc99aef5e730bceb9e4f38e0/src/main/java/net/lenni0451/reflect/JavaBypass.java
        // licensed under MIT

        Unsafe unsafe = null;
        for (Field field : Unsafe.class.getDeclaredFields()) {
            if (field.getType() == Unsafe.class) {
                try {
                    field.trySetAccessible();
                    unsafe = (Unsafe) field.get(null);
                } catch (ReflectiveOperationException exception) {
                    throw new RuntimeException(exception);
                }
                break;
            }
        }
        THE_UNSAFE = Objects.requireNonNull(unsafe, "Can't find unsafe instance");

        try {
            // obtain trusted lookup
            MethodHandles.lookup(); // load class before getting the trusted lookup
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long implLookupFieldOffset = THE_UNSAFE.staticFieldOffset(implLookupField);
            TRUSTED_LOOKUP = (MethodHandles.Lookup) THE_UNSAFE.getObject(MethodHandles.Lookup.class, implLookupFieldOffset);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static MethodHandles.Lookup getTrustedLookup() {
        return TRUSTED_LOOKUP;
    }

    public static MethodHandle getGetter(Class<?> clazz, Class<?> type, int offset) {
        return getGetter(lookupField(clazz, type, offset));
    }

    public static MethodHandle getGetter(Field field) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return TRUSTED_LOOKUP.findStaticGetter(field.getDeclaringClass(), field.getName(), field.getType());
            }
            return TRUSTED_LOOKUP.findGetter(field.getDeclaringClass(), field.getName(), field.getType());
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static MethodHandle getSetter(Class<?> clazz, Class<?> type, int offset) {
        return getSetter(lookupField(clazz, type, offset));
    }

    public static MethodHandle getSetter(Field field) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return TRUSTED_LOOKUP.findStaticSetter(field.getDeclaringClass(), field.getName(), field.getType());
            }
            return TRUSTED_LOOKUP.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static Field lookupField(Class<?> clazz, Class<?> type, int offset) {
        int i = 0;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() != type) {
                continue;
            }
            if (i++ == offset) {
                return field;
            }
        }
        throw new IllegalArgumentException("Can't find field " + type
                + " with offset " + offset + " in " + clazz.getName());
    }
}
