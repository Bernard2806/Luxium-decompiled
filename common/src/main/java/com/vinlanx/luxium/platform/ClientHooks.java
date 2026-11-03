package com.vinlanx.luxium.platform;

import com.vinlanx.luxium.luxiumMod;
import com.vinlanx.luxium.Testing.AirDistortion;
import com.vinlanx.luxium.Testing.BlackHole;
import com.vinlanx.luxium.Testing.Grass;
import com.vinlanx.luxium.Testing.LuxiumMasterKeyHandler;
import com.vinlanx.luxium.Testing.ShockWaveTest;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionRenderer;
import com.vinlanx.luxium.client.GodRaySystem;
import com.vinlanx.luxium.client.LightAtlas;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.kawase.KawaseBloomRenderer;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleRenderer;
import com.vinlanx.luxium.client.wet.WetSurfaceRenderer; public final class ClientHooks { private ClientHooks() { } public static void tick() { luxiumMod.onClientTick(); LuxiumMasterKeyHandler.onClientTick(); TestFlashLight.onClientTick(); AirDistortion.onClientTick(); ShockWaveTest.onClientTick(); BlackHole.onClientTick(); } public static void renderHighest(RenderFrame frame) { SharedPostResources.captureSharedBuffers(frame); WetSurfaceRenderer.captureSolidTerrainDepth(frame); WetSurfaceRenderer.renderWetSurfaces(frame); }

    public static void renderHigh(RenderFrame frame) { NeoShadowsEngine.onRenderLevel(frame);
    }

    public static void renderNormal(RenderFrame frame) {
        ReflectionSystem.onRenderLevel(frame);
        LightAtlas.onRenderLevel(frame);
        Grass.onRenderLevel(frame);
        RainPuddleRenderer.onRenderLevel(frame);
    }

    public static void renderLow(RenderFrame frame) {
        GodRaySystem.onRenderLevel(frame);
        AirDistortion.onRenderLevel(frame);
        ShockWaveTest.onRenderLevel(frame);
        BlackHole.onRenderLevel(frame);
        NeoCpuShadowsEngine.onRenderLevel(frame);
        KawaseBloomRenderer.render(frame);
        ExplosionRenderer.render(frame);
    }

    public static void renderLowest(RenderFrame frame) {
        PostEffectPipeline.onRenderLevel(frame);
    }

    public static void render(RenderFrame frame) {
        renderHighest(frame);
        renderHigh(frame);
        renderNormal(frame);
        renderLow(frame);
        renderLowest(frame);
    }
    }
