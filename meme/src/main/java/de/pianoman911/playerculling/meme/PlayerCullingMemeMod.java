package de.pianoman911.playerculling.meme;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;

public class PlayerCullingMemeMod implements ClientModInitializer {

    private static final ResourceLocation DUMMY = ResourceLocation.parse("playercullingmeme:dummy");

    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloader(DUMMY, (sharedState, exectutor, barrier, applyExectutor) ->
                        barrier.wait(exectutor).thenRunAsync(this::extractRenderStates, applyExectutor));
    }

    private void extractRenderStates() {
        System.out.println("Extracting render states...");
        Minecraft instance = Minecraft.getInstance();

        ZombieRenderState state = new ZombieRenderState();
        state.entityType = EntityType.ZOMBIE;
        EntityRenderer<?, ? super HumanoidRenderState> renderer = instance.getEntityRenderDispatcher().getRenderer(state);

        if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
            EntityModel<?> model = livingEntityRenderer.getModel();
            for (ModelPart part : model.allParts()) {
                PoseStack poseStack = new PoseStack();
                part.visit(poseStack, (pose, path, index, cube) -> {
                    System.out.println(cube.minX + ", " + cube.minY + ", " + cube.minZ + " -> " +
                            cube.maxX + ", " + cube.maxY + ", " + cube.maxZ);
                });
            }
        }
    }
}
