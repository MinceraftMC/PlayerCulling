package de.pianoman911.playerculling.meme.wrapped;

import java.lang.invoke.MethodHandle;

public abstract class AbstractWrapped {

    protected Class<?> wrappedClass;

    public static class Instance {

        protected final Object instance;

        public Instance(Object instance) {
            this.instance = instance;
        }

        public Object getInstance() {
            return this.instance;
        }

        @SuppressWarnings("unchecked")
        protected <T> T invokeSafe(MethodHandle handle) {
            try {
                return (T) handle.invoke(this.instance);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
}
