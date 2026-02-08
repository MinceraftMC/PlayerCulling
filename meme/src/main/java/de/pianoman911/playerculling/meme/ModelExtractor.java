package de.pianoman911.playerculling.meme;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public class ModelExtractor {

    private final Map<EntityType<?>, EntityRenderer<?, ?>> renders;

    public ModelExtractor(Map<EntityType<?>, EntityRenderer<?, ?>> renders) {
        this.renders = renders;
    }

    public void extract() {
        for (Map.Entry<EntityType<?>, EntityRenderer<?, ?>> entry : this.renders.entrySet()) {
            EntityType<?> type = entry.getKey();
            EntityRenderer<?, ?> renderer = entry.getValue();
            this.extractModel(type, renderer);
        }
    }

    private void extractModel(EntityType<?> type, EntityRenderer<?, ?> renderer) {
        EntityRenderState state = renderer.createRenderState();
        state.entityType = type;
    }
}
