/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaMath;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyDepthFramebuffer;
import com.vinlanx.luxium.mixin.EntityRenderDispatcherAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class NeoSkyEntityShadowMap
implements AutoCloseable {
    private final NeoSkyDepthFramebuffer target = new NeoSkyDepthFramebuffer();
    private final Matrix4f lightViewRotation = new Matrix4f();
    private final Matrix4f lightProjection = new Matrix4f();
    private final Matrix4f lightFromMainView = new Matrix4f();
    private Vec3 eye = Vec3.f_82478_;
    private int resolution;
    private float radius;
    private long lastUpdateNanos;
    private boolean ready;
    private long generation;

    public void configure(int requestedResolution, float requestedRadius) {
        int newResolution = Mth.m_14045_((int)requestedResolution, (int)512, (int)2048);
        float newRadius = Mth.m_14036_((float)requestedRadius, (float)16.0f, (float)128.0f);
        if (this.resolution == newResolution && this.radius == newRadius) {
            return;
        }
        this.resolution = newResolution;
        this.radius = newRadius;
        this.target.resize(newResolution);
        this.ready = false;
    }

    public boolean shouldUpdate(long nowNanos) {
        int updateFps = (Integer)Config.CLIENT.skyEntityShadowUpdateFps.get();
        if (updateFps >= 61) {
            return true;
        }
        long interval = 1000000000L / (long)Mth.m_14045_((int)updateFps, (int)10, (int)60);
        return !this.ready || nowNanos - this.lastUpdateNanos >= interval;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(Minecraft mc, Vec3 center, Vec3 toLight, float partialTick, long nowNanos) {
        Matrix4f rotation = NeoSkyCelestiaMath.buildLightViewRotation(toLight);
        Vec3 snappedCenter = NeoSkyCelestiaMath.computeSnappedCenter(center, toLight, this.radius, this.resolution);
        float rayLength = ((Integer)Config.CLIENT.skyShadowRayLength.get()).intValue();
        this.eye = snappedCenter.m_82549_(toLight.m_82490_((double)rayLength));
        this.lightViewRotation.set((Matrix4fc)rotation);
        this.lightProjection.set((Matrix4fc)new Matrix4f().ortho(-this.radius, this.radius, -this.radius, this.radius, 0.25f, rayLength + this.radius * 2.5f, false));
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        EntityRenderDispatcher dispatcher = mc.m_91290_();
        boolean previousRenderShadow = ((EntityRenderDispatcherAccessor)dispatcher).luxium$shouldRenderShadow();
        boolean previousHitBoxes = dispatcher.m_114377_();
        PoseStack poseStack = new PoseStack();
        poseStack.m_252931_(this.lightViewRotation);
        MultiBufferSource.BufferSource buffers = MultiBufferSource.m_109898_((BufferBuilder)new BufferBuilder(262144));
        this.target.bindAndClear();
        NeoSkyCelestia.get().setEntityShadowPass(true);
        dispatcher.m_114468_(false);
        dispatcher.m_114473_(false);
        dispatcher.m_114408_((Level)mc.f_91073_, mc.f_91063_.m_109153_(), null);
        dispatcher.m_252923_(new Quaternionf().setFromNormalized((Matrix3fc)new Matrix3f((Matrix4fc)this.lightViewRotation).invert()));
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        RenderSystem.applyModelViewMatrix();
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)this.lightProjection, (VertexSorting)VertexSorting.f_276450_);
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)513);
            RenderSystem.disableBlend();
            RenderSystem.polygonOffset((float)1.5f, (float)3.0f);
            RenderSystem.enablePolygonOffset();
            for (Entity entity : mc.f_91073_.m_104735_()) {
                if (entity.m_213877_() || !this.intersects(entity.m_6921_().m_82400_(0.5))) continue;
                double x = Mth.m_14139_((double)partialTick, (double)entity.f_19790_, (double)entity.m_20185_()) - this.eye.f_82479_;
                double y = Mth.m_14139_((double)partialTick, (double)entity.f_19791_, (double)entity.m_20186_()) - this.eye.f_82480_;
                double z = Mth.m_14139_((double)partialTick, (double)entity.f_19792_, (double)entity.m_20189_()) - this.eye.f_82481_;
                float yaw = Mth.m_14179_((float)partialTick, (float)entity.f_19859_, (float)entity.m_146908_());
                dispatcher.m_114384_(entity, x, y, z, yaw, partialTick, poseStack, (MultiBufferSource)buffers, 0xF000F0);
            }
            buffers.m_109911_();
            this.lastUpdateNanos = nowNanos;
            this.ready = true;
            ++this.generation;
        }
        finally {
            NeoSkyCelestia.get().setEntityShadowPass(false);
            dispatcher.m_114468_(previousRenderShadow);
            dispatcher.m_114473_(previousHitBoxes);
            dispatcher.m_114408_((Level)mc.f_91073_, mc.f_91063_.m_109153_(), mc.f_91076_);
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.polygonOffset((float)0.0f, (float)0.0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)515);
            RenderSystem.disableBlend();
            mc.m_91385_().m_83947_(true);
        }
    }

    public void updateReceiverMatrix(Vec3 mainCamera, Matrix4f inverseMainViewRotation) {
        if (!this.ready) {
            return;
        }
        this.lightFromMainView.set((Matrix4fc)NeoSkyCelestiaMath.buildLightFromMainView(mainCamera, this.eye, this.lightViewRotation, this.lightProjection, inverseMainViewRotation));
    }

    private boolean intersects(AABB bounds) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        Vector3f corner = new Vector3f();
        for (int mask = 0; mask < 8; ++mask) {
            corner.set((float)(((mask & 1) == 0 ? bounds.f_82288_ : bounds.f_82291_) - this.eye.f_82479_), (float)(((mask & 2) == 0 ? bounds.f_82289_ : bounds.f_82292_) - this.eye.f_82480_), (float)(((mask & 4) == 0 ? bounds.f_82290_ : bounds.f_82293_) - this.eye.f_82481_));
            this.lightViewRotation.transformDirection(corner);
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        float far = (float)((Integer)Config.CLIENT.skyShadowRayLength.get()).intValue() + this.radius * 2.5f;
        return maxX >= -this.radius && minX <= this.radius && maxY >= -this.radius && minY <= this.radius && maxZ >= -far && minZ <= -0.25f;
    }

    public void invalidate() {
        this.ready = false;
        this.lastUpdateNanos = 0L;
    }

    public void bindTarget() {
        this.target.bind();
    }

    public boolean ready() {
        return this.ready;
    }

    public int depthTextureId() {
        return this.target.depthTextureId();
    }

    public Matrix4f lightFromMainView() {
        return this.lightFromMainView;
    }

    public float radius() {
        return this.radius;
    }

    public float texelSize() {
        return 1.0f / (float)Math.max(1, this.resolution);
    }

    public float baseBias() {
        return Math.max(3.5E-4f, this.radius * 2.0f / (float)this.resolution * 8.0E-4f);
    }

    public long generation() {
        return this.generation;
    }

    @Override
    public void close() {
        this.target.close();
        this.invalidate();
    }
}

