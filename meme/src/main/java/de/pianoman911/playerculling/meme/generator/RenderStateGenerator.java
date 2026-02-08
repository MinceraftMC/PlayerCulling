package de.pianoman911.playerculling.meme.generator;

import de.pianoman911.playerculling.common.ReflectionUtil;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.function.Consumer;

public class RenderStateGenerator {

    private final

    public static void randomize(EntityRenderState state, Consumer<EntityRenderState> fuzzer) {
        ReflectionUtil.FieldAccessor<?>[] fieldAccessors = ReflectionUtil.createNonStaticFieldAccessors(state.getClass());
        for (ReflectionUtil.FieldAccessor<?> accessors : fieldAccessors) {
            Class<?> type = accessors.field().getType();

        }
    }
}
