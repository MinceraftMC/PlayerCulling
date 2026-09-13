package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;

import java.lang.invoke.MethodHandle;

public class WrappedCubeDeformation extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getGrowX;
        private final MethodHandle getGrowY;
        private final MethodHandle getGrowZ;

        public Instance(Object instance) {
            super(instance);

            this.getGrowX = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "growX"));
            this.getGrowY = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "growY"));
            this.getGrowZ = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "growZ"));
        }

        public float getGrowX() {
            return this.invokeSafe(this.getGrowX);
        }

        public float getGrowY() {
            return this.invokeSafe(this.getGrowY);
        }

        public float getGrowZ() {
            return this.invokeSafe(this.getGrowZ);
        }
    }
}
