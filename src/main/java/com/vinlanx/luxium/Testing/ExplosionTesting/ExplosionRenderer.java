/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionRenderSnapshot;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionShader;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionWorld;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ExplosionRenderer {
    private static final ResourceLocation[] DUST_SPRITES = new ResourceLocation[8];
    private static final TextureAtlasSprite[] CACHED_SPRITES = new TextureAtlasSprite[8];
    private static TextureAtlas cachedAtlas;
    private static long lastFrameNanos;

    private ExplosionRenderer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        ExplosionRenderSnapshot snapshot;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        float dt = ExplosionRenderer.frameDelta();
        ExplosionWorld world = ExplosionWorld.get();
        world.requestStep(dt);
        ExplosionRenderSnapshot explosionRenderSnapshot = snapshot = world.snapshot();
        synchronized (explosionRenderSnapshot) {
            if (snapshot.totalCount == 0) {
                return;
            }
            ShaderInstance shader = ExplosionShader.get();
            if (shader == null) {
                return;
            }
            Vec3 camera = event.getCamera().m_90583_();
            Vector3f up = event.getCamera().m_253028_();
            Vector3f right = new Vector3f((Vector3fc)event.getCamera().m_252775_()).negate();
            PoseStack poseStack = event.getPoseStack();
            poseStack.m_85836_();
            poseStack.m_85837_(-camera.f_82479_, -camera.f_82480_, -camera.f_82481_);
            Matrix4f pose = poseStack.m_85850_().m_252922_();
            int savedTexture0 = RenderSystem.getShaderTexture((int)0);
            float[] savedColor = RenderSystem.getShaderColor();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)TextureAtlas.f_118260_);
            try {
                ExplosionRenderer.drawBatch(mc, shader, snapshot, 0, snapshot.smokeCount, pose, right, up, false);
                ExplosionRenderer.drawBatch(mc, shader, snapshot, snapshot.smokeCount, snapshot.totalCount, pose, right, up, true);
            }
            finally {
                shader.m_173362_();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
                RenderSystem.depthMask((boolean)true);
                RenderSystem.depthFunc((int)515);
                RenderSystem.setShaderColor((float)savedColor[0], (float)savedColor[1], (float)savedColor[2], (float)savedColor[3]);
                RenderSystem.setShaderTexture((int)0, (int)savedTexture0);
                RenderSystem.activeTexture((int)33984);
                poseStack.m_85849_();
            }
        }
    }

    private static void drawBatch(Minecraft mc, ShaderInstance shader, ExplosionRenderSnapshot snapshot, int start, int end, Matrix4f pose, Vector3f right, Vector3f up, boolean emissive) {
        if (start >= end) {
            return;
        }
        if (shader.m_173348_("EmissivePass") != null) {
            shader.m_173348_("EmissivePass").m_5985_(emissive ? 1.0f : 0.0f);
        }
        if (shader.f_173308_ != null) {
            shader.f_173308_.m_5679_(RenderSystem.getModelViewMatrix());
        }
        if (shader.f_173309_ != null) {
            shader.f_173309_.m_5679_(RenderSystem.getProjectionMatrix());
        }
        TextureAtlas particleAtlas = (TextureAtlas)mc.m_91097_().m_118506_(TextureAtlas.f_118260_);
        ExplosionRenderer.cacheSprites(particleAtlas);
        shader.m_173350_("Sampler0", (Object)particleAtlas);
        shader.m_173363_();
        GlStateManager._blendFunc((int)770, (int)(emissive ? 1 : 771));
        BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
        buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85820_);
        for (int index = start; index < end; ++index) {
            TextureAtlasSprite sprite = CACHED_SPRITES[snapshot.frame[index] & 7];
            float half = snapshot.size[index] * 0.5f;
            float rx = right.x * half;
            float ry = right.y * half;
            float rz = right.z * half;
            float ux = up.x * half;
            float uy = up.y * half;
            float uz = up.z * half;
            float hdrScale = Math.max(1.0f, Math.max(snapshot.r[index], Math.max(snapshot.g[index], snapshot.b[index])));
            int red = Math.round(ExplosionRenderer.MthClamp(snapshot.r[index] / hdrScale) * 255.0f);
            int green = Math.round(ExplosionRenderer.MthClamp(snapshot.g[index] / hdrScale) * 255.0f);
            int blue = Math.round(ExplosionRenderer.MthClamp(snapshot.b[index] / hdrScale) * 255.0f);
            int alpha = Math.round(ExplosionRenderer.MthClamp(snapshot.alpha[index]) * 255.0f);
            int encodedHdr = Math.round(hdrScale * 1024.0f);
            float x = snapshot.x[index];
            float y = snapshot.y[index];
            float z = snapshot.z[index];
            ExplosionRenderer.vertex(buffer, pose, x - rx - ux, y - ry - uy, z - rz - uz, red, green, blue, alpha, sprite.m_118409_(), sprite.m_118412_(), encodedHdr);
            ExplosionRenderer.vertex(buffer, pose, x + rx - ux, y + ry - uy, z + rz - uz, red, green, blue, alpha, sprite.m_118410_(), sprite.m_118412_(), encodedHdr);
            ExplosionRenderer.vertex(buffer, pose, x + rx + ux, y + ry + uy, z + rz + uz, red, green, blue, alpha, sprite.m_118410_(), sprite.m_118411_(), encodedHdr);
            ExplosionRenderer.vertex(buffer, pose, x - rx + ux, y - ry + uy, z - rz + uz, red, green, blue, alpha, sprite.m_118409_(), sprite.m_118411_(), encodedHdr);
        }
        BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        shader.m_173362_();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float z, int r, int g, int b, int a, float u, float v, int encodedHdr) {
        buffer.m_252986_(pose, x, y, z).m_6122_(r, g, b, a).m_7421_(u, v).m_7120_(0, encodedHdr).m_5752_();
    }

    private static void cacheSprites(TextureAtlas atlas) {
        if (cachedAtlas == atlas && CACHED_SPRITES[0] != null) {
            return;
        }
        cachedAtlas = atlas;
        for (int i = 0; i < CACHED_SPRITES.length; ++i) {
            ExplosionRenderer.CACHED_SPRITES[i] = atlas.m_118316_(DUST_SPRITES[i]);
        }
    }

    private static float MthClamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float frameDelta() {
        long now = System.nanoTime();
        if (lastFrameNanos < 0L) {
            lastFrameNanos = now;
            return 0.016666668f;
        }
        float dt = (float)(now - lastFrameNanos) / 1.0E9f;
        lastFrameNanos = now;
        return Math.max(0.0f, Math.min(dt, 0.05f));
    }

    static {
        lastFrameNanos = -1L;
        for (int i = 0; i < DUST_SPRITES.length; ++i) {
            ExplosionRenderer.DUST_SPRITES[i] = ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)("generic_" + i));
        }
    }
}

