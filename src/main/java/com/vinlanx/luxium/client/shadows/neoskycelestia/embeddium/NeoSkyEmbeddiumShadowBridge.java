/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  me.jellysquid.mods.sodium.client.SodiumClientMod
 *  me.jellysquid.mods.sodium.client.gl.device.CommandList
 *  me.jellysquid.mods.sodium.client.gl.device.RenderDevice
 *  me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager
 *  me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.render.viewport.CameraTransform
 *  org.joml.Matrix4fc
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia.embeddium;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCascade;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyShadowRenderLists;
import com.vinlanx.luxium.mixin.sky.RenderSectionManagerAccessor;
import com.vinlanx.luxium.mixin.sky.SodiumWorldRendererAccessor;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.joml.Matrix4fc;

public final class NeoSkyEmbeddiumShadowBridge {
    private final NeoSkyShadowRenderLists.Cache nearCache = new NeoSkyShadowRenderLists.Cache();
    private final NeoSkyShadowRenderLists.Cache farCache = new NeoSkyShadowRenderLists.Cache();
    private int frameId;

    public boolean isAvailable() {
        return SodiumWorldRenderer.instanceNullable() != null;
    }

    public PreparedRenderLists prepareRenderLists(NeoSkyCascade near, boolean buildNear, NeoSkyCascade far, boolean buildFar) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null) {
            return null;
        }
        RenderSectionManager manager = ((SodiumWorldRendererAccessor)worldRenderer).luxium$getRenderSectionManager();
        if (manager == null) {
            return null;
        }
        RenderSectionManagerAccessor accessor = (RenderSectionManagerAccessor)manager;
        NeoSkyShadowRenderLists.BuildResult lists = NeoSkyShadowRenderLists.build(accessor.luxium$getSectionByPosition(), near, buildNear, far, buildFar, ++this.frameId, this.nearCache, this.farCache);
        return new PreparedRenderLists(accessor.luxium$getChunkRenderer(), lists.near(), lists.nearCasterFront(), lists.nearIncomplete(), lists.far(), lists.farCasterFront(), lists.farIncomplete());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean renderCascade(NeoSkyCascade cascade, ChunkRenderer renderer, NeoSkyShadowRenderLists lists) {
        if (renderer == null || lists == null) {
            return false;
        }
        if (lists.regionCount() == 0) {
            return true;
        }
        ChunkRenderMatrices matrices = new ChunkRenderMatrices((Matrix4fc)cascade.lightProjection(), (Matrix4fc)cascade.lightViewRotation());
        CameraTransform camera = new CameraTransform(cascade.eye().f_82479_, cascade.eye().f_82480_, cascade.eye().f_82481_);
        RenderDevice.enterManagedCode();
        CommandList commandList = null;
        boolean previousBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
        SodiumClientMod.options().performance.useBlockFaceCulling = false;
        try {
            commandList = RenderDevice.INSTANCE.createCommandList();
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)513);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            renderer.render(matrices, commandList, (ChunkRenderListIterable)lists, DefaultTerrainRenderPasses.SOLID, camera);
            renderer.render(matrices, commandList, (ChunkRenderListIterable)lists, DefaultTerrainRenderPasses.CUTOUT, camera);
            boolean bl = true;
            return bl;
        }
        finally {
            SodiumClientMod.options().performance.useBlockFaceCulling = previousBlockFaceCulling;
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            if (commandList != null) {
                commandList.close();
            }
            RenderDevice.exitManagedCode();
        }
    }

    public void clearCaches() {
        this.nearCache.clear();
        this.farCache.clear();
    }

    public record PreparedRenderLists(ChunkRenderer renderer, NeoSkyShadowRenderLists near, float nearCasterFront, boolean nearIncomplete, NeoSkyShadowRenderLists far, float farCasterFront, boolean farIncomplete) {
    }
}

