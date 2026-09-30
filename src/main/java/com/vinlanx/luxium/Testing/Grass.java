/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.shaders.Uniform
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexBuffer
 *  com.mojang.blaze3d.vertex.VertexBuffer$Usage
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.BiomeColors
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.Testing;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class Grass {
    private static final int SIN_BITS = 12;
    private static final int SIN_MASK = 4095;
    private static final int SIN_SIZE = 4096;
    private static final float SIN_TO_IDX = 651.8986f;
    private static final float[] SIN_TABLE = new float[4096];
    public static final int BLADES_PER_BLOCK_MIN = 30;
    public static final int BLADES_PER_BLOCK_MAX = 45;
    private static final float BLADE_HEIGHT = 0.5f;
    private static final float TWO_PI = (float)Math.PI * 2;
    private static final int COLOR_VARIATION = 12;
    private static final float WIND_DIR_DRIFT = 0.018f;
    private static final float WIND_DIR_OFFSET = 0.65f;
    private static final long ANIMATION_START_NANOS;
    private static final ResourceLocation TEXTURE;
    private static final int LIGHT_REFRESH_TICKS = 40;
    private static final ExecutorService ASYNC;
    private static final AtomicBoolean asyncBusy;
    private static final AtomicReference<BufferBuilder.RenderedBuffer> pendingUpload;
    private static volatile ScanData currentScan;
    private static VertexBuffer grassVbo;
    private static volatile boolean enabled;
    private static volatile int renderRadius;
    private static volatile boolean needsRescan;
    private static long lastLightRefreshTick;
    private static float windDirX;
    private static float windDirZ;

    private Grass() {
    }

    private static float fastSin(float angle) {
        return SIN_TABLE[(int)(angle * 651.8986f) & 0xFFF];
    }

    public static void enable() {
        enabled = true;
        needsRescan = true;
    }

    public static void disable() {
        enabled = false;
        currentScan = null;
        pendingUpload.set(null);
        if (grassVbo != null) {
            grassVbo.close();
            grassVbo = null;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setRenderRadius(int r) {
        renderRadius = Math.max(1, Math.min(100, r));
        needsRescan = true;
    }

    public static int getRenderRadius() {
        return renderRadius;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        BufferBuilder.RenderedBuffer ready;
        long gameTick;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!enabled) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass()) {
            return;
        }
        ShaderInstance shader = ShaderManager.getGrassShader();
        if (shader == null) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        if (needsRescan) {
            needsRescan = false;
            Grass.scheduleAsyncScan(mc);
        }
        if ((gameTick = mc.f_91073_.m_46467_()) - lastLightRefreshTick >= 40L) {
            lastLightRefreshTick = gameTick;
            ScanData sd = currentScan;
            if (sd != null) {
                Grass.refreshLightInPlace(mc, sd);
                Grass.scheduleAsyncBuild(sd);
            }
        }
        if ((ready = (BufferBuilder.RenderedBuffer)pendingUpload.getAndSet(null)) != null) {
            if (grassVbo == null) {
                grassVbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }
            grassVbo.m_85921_();
            grassVbo.m_231221_(ready);
            VertexBuffer.m_85931_();
        }
        if (grassVbo == null) {
            return;
        }
        float time = (float)(System.nanoTime() - ANIMATION_START_NANOS) * 1.0E-9f;
        float windAngle = time * 0.018f + 0.65f;
        windDirX = Grass.fastSin(windAngle + 1.5707964f);
        windDirZ = Grass.fastSin(windAngle);
        Vec3 cam = event.getCamera().m_90583_();
        Grass.setGrassUniforms(shader, time, cam);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)TEXTURE);
        mc.f_91063_.m_109154_().m_109896_();
        PoseStack ps = event.getPoseStack();
        ps.m_85836_();
        ps.m_85837_(-cam.f_82479_, -cam.f_82480_, -cam.f_82481_);
        grassVbo.m_85921_();
        grassVbo.m_253207_(ps.m_85850_().m_252922_(), RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.m_85931_();
        ps.m_85849_();
        mc.f_91063_.m_109154_().m_109891_();
        RenderSystem.enableCull();
    }

    private static void scheduleAsyncScan(Minecraft mc) {
        if (!asyncBusy.compareAndSet(false, true)) {
            return;
        }
        int radius = renderRadius;
        BlockPos origin = mc.f_91074_.m_20183_();
        ClientLevel levelRef = mc.f_91073_;
        ASYNC.submit(() -> {
            try {
                ScanData sd;
                currentScan = sd = Grass.doScan(levelRef, origin, radius);
                pendingUpload.set(Grass.doFill(sd));
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
            finally {
                asyncBusy.set(false);
            }
        });
    }

    private static void scheduleAsyncBuild(ScanData sd) {
        if (!asyncBusy.compareAndSet(false, true)) {
            return;
        }
        int[] ls = (int[])sd.blockPackedLight.clone();
        ScanData snap = new ScanData(sd.totalBlades, sd.totalBlocks, sd.bladePx, sd.bladePz, sd.bladeBaseY, sd.bladePhi1, sd.bladeCR, sd.bladeCG, sd.bladeCB, sd.bladeBlkIdx, sd.blockAbovePos, ls);
        ASYNC.submit(() -> {
            try {
                pendingUpload.set(Grass.doFill(snap));
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
            finally {
                asyncBusy.set(false);
            }
        });
    }

    private static void setGrassUniforms(ShaderInstance s, float time, Vec3 camera) {
        Uniform u = s.m_173348_("Time");
        if (u != null) {
            u.m_5985_(time);
        }
        if ((u = s.m_173348_("WindDirX")) != null) {
            u.m_5985_(windDirX);
        }
        if ((u = s.m_173348_("WindDirZ")) != null) {
            u.m_5985_(windDirZ);
        }
        if ((u = s.m_173348_("CameraX")) != null) {
            u.m_5985_((float)camera.f_82479_);
        }
        if ((u = s.m_173348_("CameraZ")) != null) {
            u.m_5985_((float)camera.f_82481_);
        }
    }

    private static ScanData doScan(ClientLevel level, BlockPos origin, int radius) {
        int minY = origin.m_123342_() - 5;
        int maxY = origin.m_123342_() + 4;
        int blockCount = 0;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                for (int by = minY; by <= maxY; ++by) {
                    BlockPos p = new BlockPos(origin.m_123341_() + dx, by, origin.m_123343_() + dz);
                    if (!level.m_8055_(p).m_60713_(Blocks.f_50440_) || !level.m_8055_(p.m_7494_()).m_60795_()) continue;
                    ++blockCount;
                }
            }
        }
        long[] blockAbovePos = new long[blockCount];
        int[] blockPackedLight = new int[blockCount];
        int maxBlades = blockCount * 45;
        float[] tPx = new float[maxBlades];
        float[] tPz = new float[maxBlades];
        float[] tBaseY = new float[maxBlades];
        float[] tPhi1 = new float[maxBlades];
        int[] tCR = new int[maxBlades];
        int[] tCG = new int[maxBlades];
        int[] tCB = new int[maxBlades];
        int[] tBlkIdx = new int[maxBlades];
        int blkIdx = 0;
        int bladeIdx = 0;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                int bx = origin.m_123341_() + dx;
                int bz = origin.m_123343_() + dz;
                for (int by = minY; by <= maxY; ++by) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (!level.m_8055_(pos).m_60713_(Blocks.f_50440_) || !level.m_8055_(pos.m_7494_()).m_60795_()) continue;
                    BlockPos above = pos.m_7494_();
                    blockAbovePos[blkIdx] = above.m_121878_();
                    blockPackedLight[blkIdx] = LightTexture.m_109885_((int)level.m_45517_(LightLayer.BLOCK, above), (int)level.m_45517_(LightLayer.SKY, above));
                    int color = BiomeColors.m_108793_((BlockAndTintGetter)level, (BlockPos)pos);
                    int cr = color >> 16 & 0xFF;
                    int cg = color >> 8 & 0xFF;
                    int cb = color & 0xFF;
                    long blockSeed = pos.m_121878_();
                    float hOffX = (float)((double)(blockSeed * -7046029254386353131L >>> 33) / 2.147483648E9);
                    float hOffZ = (float)((double)(blockSeed * 7809847782465536322L >>> 33) / 2.147483648E9);
                    int bladeCount = Grass.bladesForBlock(blockSeed);
                    for (int i = 0; i < bladeCount; ++i) {
                        tPx[bladeIdx] = (float)bx + Grass.frac(Grass.halton2(i) + hOffX);
                        tPz[bladeIdx] = (float)bz + Grass.frac(Grass.halton3(i) + hOffZ);
                        tBaseY[bladeIdx] = (float)by + 1.0f;
                        long seed = blockSeed * 31L + (long)i * 197L;
                        float bs = (float)((double)(seed & 0xFFFFFFFFL) / 4.294967296E9);
                        tPhi1[bladeIdx] = bs * ((float)Math.PI * 2);
                        tCR[bladeIdx] = cr;
                        tCG[bladeIdx] = cg;
                        tCB[bladeIdx] = cb;
                        tBlkIdx[bladeIdx] = blkIdx;
                        ++bladeIdx;
                    }
                    ++blkIdx;
                }
            }
        }
        return new ScanData(bladeIdx, blkIdx, Grass.trim(tPx, bladeIdx), Grass.trim(tPz, bladeIdx), Grass.trim(tBaseY, bladeIdx), Grass.trim(tPhi1, bladeIdx), Grass.trimInt(tCR, bladeIdx), Grass.trimInt(tCG, bladeIdx), Grass.trimInt(tCB, bladeIdx), Grass.trimInt(tBlkIdx, bladeIdx), blockAbovePos, blockPackedLight);
    }

    private static BufferBuilder.RenderedBuffer doFill(ScanData sd) {
        BufferBuilder bb = new BufferBuilder(0x800000);
        bb.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85812_);
        float H = 0.5f;
        int V = 12;
        for (int idx = 0; idx < sd.totalBlades; ++idx) {
            int phi1Enc;
            float bx = sd.bladePx[idx];
            float bz = sd.bladePz[idx];
            float base = sd.bladeBaseY[idx];
            float phi1 = sd.bladePhi1[idx];
            int light = sd.blockPackedLight[sd.bladeBlkIdx[idx]];
            int hash = Float.floatToRawIntBits(phi1);
            int vr = (hash * -1640531527 >>> 24 & 0xFF) * 2 * 12 / 255 - 12;
            int vg = (hash * 1818371887 >>> 24 & 0xFF) * 2 * 12 / 255 - 12;
            int vb = (hash * 608135817 >>> 24 & 0xFF) * 2 * 12 / 255 - 12;
            int r = Math.max(0, Math.min(255, sd.bladeCR[idx] + vr));
            int g = Math.max(0, Math.min(255, sd.bladeCG[idx] + vg));
            int b = Math.max(0, Math.min(255, sd.bladeCB[idx] + vb));
            int ov_left = phi1Enc = (int)(phi1 / ((float)Math.PI * 2) * 32767.0f) & Short.MAX_VALUE;
            int ov_right = phi1Enc | 0x10000;
            Grass.emitQuad(bb, bx, bz, base, 1.0f, base + 0.125f, 0.75f, r, g, b, light, ov_left, ov_right);
            Grass.emitQuad(bb, bx, bz, base + 0.125f, 0.75f, base + 0.25f, 0.5f, r, g, b, light, ov_left, ov_right);
            Grass.emitQuad(bb, bx, bz, base + 0.25f, 0.5f, base + 0.375f, 0.25f, r, g, b, light, ov_left, ov_right);
            Grass.emitQuad(bb, bx, bz, base + 0.375f, 0.25f, base + 0.5f, 0.0f, r, g, b, light, ov_left, ov_right);
        }
        return bb.m_231175_();
    }

    private static void emitQuad(BufferBuilder bb, float bx, float bz, float yBot, float uvBot, float yTop, float uvTop, int r, int g, int b, int light, int ov_left, int ov_right) {
        bb.m_5483_((double)bx, (double)yBot, (double)bz).m_6122_(r, g, b, 255).m_7421_(0.0f, uvBot).m_86008_(ov_left).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        bb.m_5483_((double)bx, (double)yBot, (double)bz).m_6122_(r, g, b, 255).m_7421_(1.0f, uvBot).m_86008_(ov_right).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        bb.m_5483_((double)bx, (double)yTop, (double)bz).m_6122_(r, g, b, 255).m_7421_(1.0f, uvTop).m_86008_(ov_right).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        bb.m_5483_((double)bx, (double)yTop, (double)bz).m_6122_(r, g, b, 255).m_7421_(0.0f, uvTop).m_86008_(ov_left).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
    }

    private static void refreshLightInPlace(Minecraft mc, ScanData sd) {
        if (mc.f_91073_ == null) {
            return;
        }
        for (int i = 0; i < sd.totalBlocks; ++i) {
            BlockPos p = BlockPos.m_122022_((long)sd.blockAbovePos[i]);
            sd.blockPackedLight[i] = LightTexture.m_109885_((int)mc.f_91073_.m_45517_(LightLayer.BLOCK, p), (int)mc.f_91073_.m_45517_(LightLayer.SKY, p));
        }
    }

    private static float halton2(int idx) {
        float r = 0.0f;
        float f = 1.0f;
        for (int i = idx + 1; i > 0; i >>= 1) {
            r += (f *= 0.5f) * (float)(i & 1);
        }
        return r;
    }

    private static float halton3(int idx) {
        float r = 0.0f;
        float f = 1.0f;
        for (int i = idx + 1; i > 0; i /= 3) {
            r += (f /= 3.0f) * (float)(i % 3);
        }
        return r;
    }

    private static float frac(float x) {
        return x - (float)Math.floor(x);
    }

    private static int bladesForBlock(long seed) {
        int range = 16;
        return 30 + Math.floorMod(Long.hashCode(seed), range);
    }

    private static float[] trim(float[] src, int len) {
        float[] r = new float[len];
        System.arraycopy(src, 0, r, 0, len);
        return r;
    }

    private static int[] trimInt(int[] src, int len) {
        int[] r = new int[len];
        System.arraycopy(src, 0, r, 0, len);
        return r;
    }

    static {
        for (int i = 0; i < 4096; ++i) {
            Grass.SIN_TABLE[i] = (float)Math.sin((double)i * 0.0015339807878856412);
        }
        ANIMATION_START_NANOS = System.nanoTime();
        TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"textures/grass.png");
        ASYNC = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "luxium-GrassBuilder");
            t.setDaemon(true);
            t.setPriority(4);
            return t;
        });
        asyncBusy = new AtomicBoolean(false);
        pendingUpload = new AtomicReference();
        currentScan = null;
        grassVbo = null;
        enabled = false;
        renderRadius = 12;
        needsRescan = false;
        lastLightRefreshTick = -999L;
        windDirX = 1.0f;
        windDirZ = 0.0f;
    }

    private static final class ScanData {
        final int totalBlades;
        final int totalBlocks;
        final float[] bladePx;
        final float[] bladePz;
        final float[] bladeBaseY;
        final float[] bladePhi1;
        final int[] bladeCR;
        final int[] bladeCG;
        final int[] bladeCB;
        final int[] bladeBlkIdx;
        final long[] blockAbovePos;
        final int[] blockPackedLight;

        ScanData(int totalBlades, int totalBlocks, float[] px, float[] pz, float[] baseY, float[] phi1, int[] cr, int[] cg, int[] cb, int[] blkIdx, long[] abovePos, int[] packedLight) {
            this.totalBlades = totalBlades;
            this.totalBlocks = totalBlocks;
            this.bladePx = px;
            this.bladePz = pz;
            this.bladeBaseY = baseY;
            this.bladePhi1 = phi1;
            this.bladeCR = cr;
            this.bladeCG = cg;
            this.bladeCB = cb;
            this.bladeBlkIdx = blkIdx;
            this.blockAbovePos = abovePos;
            this.blockPackedLight = packedLight;
        }
    }
}

