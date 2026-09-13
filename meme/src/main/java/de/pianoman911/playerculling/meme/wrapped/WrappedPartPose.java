package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;

import java.lang.invoke.MethodHandle;

public class WrappedPartPose extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getX;
        private final MethodHandle getY;
        private final MethodHandle getZ;
        private final MethodHandle getXRot;
        private final MethodHandle getYRot;
        private final MethodHandle getZRot;
        private final MethodHandle getXScale;
        private final MethodHandle getYScale;
        private final MethodHandle getZScale;

        public Instance(Object instance) {
            super(instance);

            this.getX = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "x"));
            this.getY = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "y"));
            this.getZ = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "z"));
            this.getXRot = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "xRot"));
            this.getYRot = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "yRot"));
            this.getZRot = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "zRot"));
            this.getXScale = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "xScale"));
            this.getYScale = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "yScale"));
            this.getZScale = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "zScale"));
        }

        public float getX() {
            return this.invokeSafe(this.getX);
        }

        public float getY() {
            return this.invokeSafe(this.getY);
        }

        public float getZ() {
            return this.invokeSafe(this.getZ);
        }

        public float getXRot() {
            return this.invokeSafe(this.getXRot);
        }

        public float getYRot() {
            return this.invokeSafe(this.getYRot);
        }

        public float getZRot() {
            return this.invokeSafe(this.getZRot);
        }

        public float getXScale() {
            return this.invokeSafe(this.getXScale);
        }

        public float getYScale() {
            return this.invokeSafe(this.getYScale);
        }

        public float getZScale() {
            return this.invokeSafe(this.getZScale);
        }
    }
}
