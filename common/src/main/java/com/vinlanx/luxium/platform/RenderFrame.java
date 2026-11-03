package com.vinlanx.luxium.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public record RenderFrame(
    RenderFrame.Stage stage, PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, float partialTick, Camera camera, Frustum frustum ) {
    public static enum Stage {
    AFTER_SKY, AFTER_SOLID_BLOCKS, AFTER_CUTOUT_MIPPED_BLOCKS, AFTER_CUTOUT_BLOCKS,
 AFTER_ENTITIES, AFTER_BLOCK_ENTITIES, AFTER_TRANSLUCENT_BLOCKS,
 AFTER_TRIPWIRE_BLOCKS, AFTER_PARTICLES, AFTER_WEATHER, AFTER_LEVEL; } public RenderFrame.Stage getStage() {


        return this.stage;
    } public PoseStack getPoseStack() { return this.poseStack;
    } public Matrix4f getProjectionMatrix() { return this.projectionMatrix;
    } public int getRenderTick() { return this.renderTick;
    } public float getPartialTick() { return this.partialTick;
    } public Camera getCamera() { return this.camera;
    } public Frustum getFrustum() { return this.frustum; }
}
