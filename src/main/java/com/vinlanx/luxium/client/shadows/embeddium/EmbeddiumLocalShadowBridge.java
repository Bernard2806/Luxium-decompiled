/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  it.unimi.dsi.fastutil.longs.Long2ReferenceMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap
 *  me.jellysquid.mods.sodium.client.gl.device.CommandList
 *  me.jellysquid.mods.sodium.client.gl.device.RenderDevice
 *  me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSection
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager
 *  me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList
 *  me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable
 *  me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.render.viewport.CameraTransform
 *  net.minecraft.core.SectionPos
 *  net.minecraft.util.Mth
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package com.vinlanx.luxium.client.shadows.embeddium;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.mixin.sky.RenderSectionManagerAccessor;
import com.vinlanx.luxium.mixin.sky.SodiumWorldRendererAccessor;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class EmbeddiumLocalShadowBridge {
    private static final int GEOMETRY_MASK = 1;
    private static final double SECTION_MARGIN = 18.0;
    private int frameId;

    public boolean isAvailable() {
        return SodiumWorldRenderer.instanceNullable() != null;
    }

    public PreparedCapture prepare(double lightX, double lightY, double lightZ, float radius) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null) {
            return null;
        }
        RenderSectionManager manager = ((SodiumWorldRendererAccessor)worldRenderer).luxium$getRenderSectionManager();
        if (manager == null) {
            return null;
        }
        RenderSectionManagerAccessor accessor = (RenderSectionManagerAccessor)manager;
        ChunkRenderer renderer = accessor.luxium$getChunkRenderer();
        Long2ReferenceMap<RenderSection> sections = accessor.luxium$getSectionByPosition();
        if (renderer == null || sections == null || sections.isEmpty()) {
            return null;
        }
        double reach = Math.max(1.0, (double)radius) + 18.0;
        int minSectionX = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightX - reach)));
        int minSectionY = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightY - reach)));
        int minSectionZ = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightZ - reach)));
        int maxSectionX = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightX + reach)));
        int maxSectionY = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightY + reach)));
        int maxSectionZ = SectionPos.m_123171_((int)Mth.m_14107_((double)(lightZ + reach)));
        int currentFrame = ++this.frameId;
        Reference2ObjectLinkedOpenHashMap byRegion = new Reference2ObjectLinkedOpenHashMap();
        double maxDistance = reach + 14.0;
        double maxDistanceSq = maxDistance * maxDistance;
        for (int sx = minSectionX; sx <= maxSectionX; ++sx) {
            for (int sz = minSectionZ; sz <= maxSectionZ; ++sz) {
                for (int sy = minSectionY; sy <= maxSectionY; ++sy) {
                    RenderRegion region;
                    double dz;
                    double dy;
                    double dx;
                    RenderSection section = (RenderSection)sections.get(SectionPos.m_123209_((int)sx, (int)sy, (int)sz));
                    if (section == null || !section.isBuilt() || section.isDisposed() || (section.getFlags() & 1) == 0 || (dx = (double)section.getCenterX() - lightX) * dx + (dy = (double)section.getCenterY() - lightY) * dy + (dz = (double)section.getCenterZ() - lightZ) * dz > maxDistanceSq || (region = section.getRegion()) == null) continue;
                    ChunkRenderList list = (ChunkRenderList)byRegion.get((Object)region);
                    if (list == null) {
                        list = new ChunkRenderList(region);
                        list.reset(currentFrame);
                        byRegion.put((Object)region, (Object)list);
                    }
                    list.add(section);
                }
            }
        }
        LocalRenderLists lists = new LocalRenderLists(new ArrayList<ChunkRenderList>((Collection<ChunkRenderList>)byRegion.values()));
        return new PreparedCapture(renderer, lists, lightX, lightY, lightZ);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean renderFace(PreparedCapture capture, Matrix4f projection, Matrix4f viewRotation) {
        if (capture == null || capture.renderer == null || capture.lists == null) {
            return false;
        }
        if (capture.lists.regionCount() == 0) {
            return false;
        }
        ChunkRenderMatrices matrices = new ChunkRenderMatrices((Matrix4fc)projection, (Matrix4fc)viewRotation);
        CameraTransform camera = new CameraTransform(capture.lightX, capture.lightY, capture.lightZ);
        RenderDevice.enterManagedCode();
        CommandList commandList = null;
        try {
            commandList = RenderDevice.INSTANCE.createCommandList();
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)513);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            capture.renderer.render(matrices, commandList, (ChunkRenderListIterable)capture.lists, DefaultTerrainRenderPasses.SOLID, camera);
            if (!NeoGpuVanilla.isConfiguredEnabled() || Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaCutoutEnabled)) {
                capture.renderer.render(matrices, commandList, (ChunkRenderListIterable)capture.lists, DefaultTerrainRenderPasses.CUTOUT, camera);
            }
            boolean bl = true;
            return bl;
        }
        finally {
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            if (commandList != null) {
                commandList.close();
            }
            RenderDevice.exitManagedCode();
        }
    }

    public static final class LocalRenderLists
    implements ChunkRenderListIterable {
        private final List<ChunkRenderList> lists;

        private LocalRenderLists(List<ChunkRenderList> lists) {
            this.lists = lists;
        }

        public Iterator<ChunkRenderList> iterator(boolean reverse) {
            if (!reverse) {
                return this.lists.iterator();
            }
            final ListIterator<ChunkRenderList> iterator = this.lists.listIterator(this.lists.size());
            return new Iterator<ChunkRenderList>(){

                @Override
                public boolean hasNext() {
                    return iterator.hasPrevious();
                }

                @Override
                public ChunkRenderList next() {
                    return (ChunkRenderList)iterator.previous();
                }
            };
        }

        public int regionCount() {
            return this.lists.size();
        }
    }

    public record PreparedCapture(ChunkRenderer renderer, LocalRenderLists lists, double lightX, double lightY, double lightZ) {
    }
}

