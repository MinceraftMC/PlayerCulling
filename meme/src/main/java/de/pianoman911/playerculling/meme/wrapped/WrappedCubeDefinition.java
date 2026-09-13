package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;

@NullMarked
public class WrappedCubeDefinition extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getComment;
        private final MethodHandle getOrigin;
        private final MethodHandle getDimensions;
        private final MethodHandle getGrow;
        private final MethodHandle isMirror;

        public Instance(Object instance) {
            super(instance);

            this.getComment = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "comment"));
            this.getOrigin = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "origin"));
            this.getDimensions = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "dimensions"));
            this.getGrow = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "grow"));
            this.isMirror = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "mirror"));
        }

        @Nullable
        public String getComment() {
            return this.invokeSafe(this.getComment);
        }

        public Vector3fc getOrigin() {
            return this.invokeSafe(this.getOrigin);
        }

        public Vector3fc getDimensions() {
            return this.invokeSafe(this.getDimensions);
        }

        public WrappedCubeDeformation.Instance getGrow() {
            return new WrappedCubeDeformation.Instance(this.invokeSafe(this.getGrow));
        }

        public boolean isMirror() {
            return this.invokeSafe(this.isMirror);
        }
    }
}
