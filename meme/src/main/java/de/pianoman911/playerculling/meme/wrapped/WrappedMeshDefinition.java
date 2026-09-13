package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;

import java.lang.invoke.MethodHandle;

public class WrappedMeshDefinition extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getRoot;

        public Instance(Object instance) {
            super(instance);
            this.getRoot = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "root"));
        }

        public WrappedPartDefinition.Instance getRoot() {
            return new WrappedPartDefinition.Instance(this.invokeSafe(this.getRoot));
        }
    }
}
