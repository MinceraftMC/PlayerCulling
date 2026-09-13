package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;

import java.lang.invoke.MethodHandle;

public class WrappedLayerDefinition extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getMesh;

        public Instance(Object instance) {
            super(instance);

            this.getMesh = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "mesh"));
        }

        public WrappedMeshDefinition.Instance getMesh() {
            return new WrappedMeshDefinition.Instance(this.invokeSafe(this.getMesh));
        }
    }
}
