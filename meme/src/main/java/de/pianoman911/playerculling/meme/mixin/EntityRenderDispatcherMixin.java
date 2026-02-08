package de.pianoman911.playerculling.meme.mixin;

import de.pianoman911.playerculling.meme.ModelExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@NullMarked
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(
            method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("RETURN")
    )
    public void onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci) {
        ModelExtractor modelExtractor = new ModelExtractor(this.renderers);
        modelExtractor.extract();
    }
}
