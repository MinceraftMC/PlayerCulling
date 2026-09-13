package de.pianoman911.playerculling.meme.wrapped;

import de.pianoman911.playerculling.common.ReflectionUtil;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrappedPartDefinition extends AbstractWrapped {

    public static class Instance extends AbstractWrapped.Instance {

        private final MethodHandle getCubes;
        private final MethodHandle getPartPose;
        private final MethodHandle getChildren;

        public Instance(Object instance) {
            super(instance);
            this.getCubes = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "cubes"));
            this.getPartPose = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "partPose"));
            this.getChildren = ReflectionUtil.getGetter(ReflectionUtil.lookupFieldByName(instance.getClass(), "children"));
        }

        public List<WrappedCubeDefinition.Instance> getCubes() {
            List<?> cubes = invokeSafe(this.getCubes);
            List<WrappedCubeDefinition.Instance> wrappedCubes = new ArrayList<>(cubes.size());

            for (Object cube : cubes) {
                wrappedCubes.add(new WrappedCubeDefinition.Instance(cube));
            }
            return wrappedCubes;
        }

        public WrappedPartPose.Instance getPartPose() {
            return new WrappedPartPose.Instance(this.invokeSafe(this.getPartPose));
        }

        public Map<String, WrappedPartDefinition.Instance> getChildren() {
            Map<?, ?> children = invokeSafe(this.getChildren);
            Map<String, WrappedPartDefinition.Instance> wrappedChildren = new HashMap<>(children.size());

            for (Map.Entry<?, ?> entry : children.entrySet()) {
                String key = (String) entry.getKey();
                WrappedPartDefinition.Instance value = new WrappedPartDefinition.Instance(entry.getValue());
                wrappedChildren.put(key, value);
            }
            return wrappedChildren;
        }
    }
}
