package com.vinlanx.luxium.client.water;


import com.mojang.blaze3d.pipeline.RenderTarget;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import com.vinlanx.luxium.client.ssr.ScreenSpaceReflectionSystem;
import com.vinlanx.luxium.client.ssr.SsrConsumer;
import com.vinlanx.luxium.client.ssr.SsrSettings;
import com.vinlanx.luxium.client.water.WaterSurfaceState;
import com.vinlanx.luxium.client.water.WaterTerrainPass;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.Minecraft;
public final class WaterSurfaceRenderer {
    private static RenderSectionManager lateManager;
    private static ChunkRenderMatrices lateMatrices;
    private static double lateCameraX;
    private static double lateCameraY;
    private static double lateCameraZ;
    private static boolean lateFrameArmed;
    private WaterSurfaceRenderer() { }


    public static boolean renderWater(RenderSectionManager manager, ChunkRenderMatrices matrices, double cameraX, double cameraY, double cameraZ) {
        if (manager == null || !WaterSurfaceState.canRenderMainPass()) {
            WaterSurfaceRenderer.disarmLateFrame();
            return false;
        }

        Minecraft mc = Minecraft.m_91087_();
        RenderTarget main = mc.m_91385_();
        if (main.f_83915_ <= 0 || main.f_83916_ <= 0) {
            WaterSurfaceRenderer.disarmLateFrame();
            return false;
        }

        WaterSurfaceState.beginFrame();
        ScreenSpaceReflectionSystem.invalidateHit(SsrConsumer.WATER);
        SharedPostResources.prepareWaterRenderTarget(main);
        if (SharedPostResources.getWaterRenderColorTextureId() <= 0 || SharedPostResources.getWaterRenderFramebufferId
                () <= 0 || SharedPostResources.getWaterRenderWidth
                () <= 0 || SharedPostResources.getWaterRenderHeight
                () <= 0) {
            main.m_83947_(true);
            WaterSurfaceRenderer.disarmLateFrame();
            return false;
        }

        SsrSettings ssr = ScreenSpaceReflectionSystem.settings(SsrConsumer.WATER);
        boolean hitTraceAttached = false;
        if (ssr.enabled()) {
            hitTraceAttached = ScreenSpaceReflectionSystem.beginHitTrace(SsrConsumer.WATER, SharedPostResources.getWaterRenderFramebufferId

                    (), SharedPostResources.getWaterRenderWidth
                    (), SharedPostResources.getWaterRenderHeight
                    ());
        }

        int previousSceneFilter = main.f_83922_;
        if (previousSceneFilter != 9729) {
            main.m_83936_(9729);
        }

        boolean materialRendered = false;
        boolean resolved = false;
        RenderDevice.enterManagedCode();
        try {
            WaterSurfaceState.beginMaterialPass();
            try {
                manager.renderLayer(matrices, WaterTerrainPass.get(), cameraX, cameraY, cameraZ);
                materialRendered = true; }
            finally {
                WaterSurfaceState.finishMaterialPass(materialRendered);
                if (hitTraceAttached) {

                    ScreenSpaceReflectionSystem.endHitTrace(SsrConsumer.WATER, materialRendered);
                }
            }

            if (!materialRendered || !WaterSurfaceState.resolvedReady()) {
                return false;
            }

            main.m_83947_(true);
            WaterSurfaceState.beginResolvePass();
            try {
                manager.renderLayer(matrices, WaterTerrainPass.get(), cameraX, cameraY, cameraZ);
                resolved = true; }
            finally {
                WaterSurfaceState.finishResolvePass();
            }
            return true; }
        finally {
            RenderDevice.exitManagedCode();

            if (hitTraceAttached && ScreenSpaceReflectionSystem.isHitTraceActive(SsrConsumer.WATER)) {
                ScreenSpaceReflectionSystem.endHitTrace(SsrConsumer.WATER, false);
            }
            if (main.f_83922_ != previousSceneFilter) {
                main.m_83936_(previousSceneFilter);
            }
            main.m_83947_(true);

            if (resolved && ssr.enabled() && ScreenSpaceReflectionSystem.isHitReady(SsrConsumer.WATER)) {
                lateManager = manager;
                lateMatrices = matrices;
                lateCameraX = cameraX;
                lateCameraY = cameraY;
                lateCameraZ = cameraZ;
                lateFrameArmed = true;
            } else {
                WaterSurfaceRenderer.disarmLateFrame();
            }
        }
    }

    public static boolean renderLateReflections() {
        if (!lateFrameArmed || lateManager == null || lateMatrices == null) {
            return false;
        }

        SsrSettings ssr = ScreenSpaceReflectionSystem.settings(SsrConsumer.WATER);
        if (!(ssr.enabled() && WaterSurfaceState.canRenderMainPass() && ScreenSpaceReflectionSystem.isHitReady
                (SsrConsumer.WATER))) {
            WaterSurfaceRenderer.disarmLateFrame();
            return false;
        }

        Minecraft mc = Minecraft.m_91087_();

        PostEffectPipeline.flushDeferredTonemapForSceneConsumer();

        RenderTarget main = mc.m_91385_();
        if (main.f_83915_ <= 0 || main.f_83916_ <= 0 || main.m_83975_() <= 0) {
            WaterSurfaceRenderer.disarmLateFrame();
            return false;
        }

        SharedPostResources.prepareWaterRenderTarget(main);

        int previousSceneFilter = main.f_83922_;
        if (previousSceneFilter != 9729) {
            main.m_83936_(9729);
        }

        boolean reflectionRendered = false;
        RenderDevice.enterManagedCode();
        try {
            WaterSurfaceState.beginLateReflectionPass();
            try {
                lateManager.renderLayer(lateMatrices, WaterTerrainPass.get(), lateCameraX, lateCameraY, lateCameraZ);

                reflectionRendered = true; }
            finally {
                WaterSurfaceState.finishLateReflectionPass(reflectionRendered);
            }

            if (!reflectionRendered || !WaterSurfaceState.lateReflectionResolvedReady()) {
                return false;
            }

            main.m_83947_(true);
            WaterSurfaceState.beginLateResolvePass();
            try {
                lateManager.renderLayer(lateMatrices, WaterTerrainPass.get(), lateCameraX, lateCameraY, lateCameraZ);
            }
            finally {
                WaterSurfaceState.finishLateResolvePass();
            }
            return true; }
        finally {
            RenderDevice.exitManagedCode();
            if (main.f_83922_ != previousSceneFilter) {
                main.m_83936_(previousSceneFilter);
            }
            main.m_83947_(true);
            WaterSurfaceRenderer.disarmLateFrame();
        }
    }

    private static void disarmLateFrame() {
        lateFrameArmed = false;
        lateManager = null;
        lateMatrices = null;
        ScreenSpaceReflectionSystem.invalidateHit(SsrConsumer.WATER);
    }
}
