/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  com.mojang.math.Axis
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.shadows;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.shadows.GpuLocalLightResolver;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.shadows.embeddium.EmbeddiumLocalShadowBridge;
import com.vinlanx.luxium.mixin.CameraAccessor;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaGpuDebug;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class GpuShadowCache {
    private static final int DEFAULT_TILE_SIZE = 256;
    private static final int FACE_COUNT = 6;
    private static final int ALL_FACES_MASK = 63;
    private static final int ATLAS_SIZE = 4096;
    private static final int MAX_DYNAMIC_FACE_REBUILDS_PER_FRAME = 24;
    private static final double BUILD_RADIUS_PADDING = 1.25;
    private static final double DYNAMIC_CASTER_SCAN_RADIUS_PADDING = 6.0;
    private static final float SHADOW_NEAR_PLANE = 0.55f;
    private static final float SHADOW_FACE_FOV = (float)Math.toRadians(92.0);
    private static final GpuShadowCache INSTANCE = new GpuShadowCache();
    private final EmbeddiumLocalShadowBridge embeddiumLocalShadowBridge = new EmbeddiumLocalShadowBridge();
    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap();
    private final LongOpenHashSet removedSources = new LongOpenHashSet();
    private final LongOpenHashSet pendingRenderedSections = new LongOpenHashSet();
    private final LongOpenHashSet expectedRenderedSections = new LongOpenHashSet();
    private int expectedRenderedSectionWaitFrames;
    private int renderedSectionQuietFrames;
    private int renderedSectionDeferralFrames;
    private final Long2ObjectOpenHashMap<AABB> trackedDynamicCasters = new Long2ObjectOpenHashMap();
    private int tileSize = 256;
    private int tileGrid = 16;
    private int maxTiles = this.tileGrid * this.tileGrid;
    private int[] freeTiles = new int[this.maxTiles];
    private int freeTileCount;
    private long visibilityFrame;
    private long revision;
    private int atlasTextureId = -1;
    private int fboId = -1;
    private RenderTarget shadowTileTarget;
    private final Camera shadowCamera = new Camera();
    @Nullable
    private ClientLevel trackedDynamicLevel;
    @Nullable
    private BlockPos trackedDynamicCenter;
    private long lastDynamicScanMs;

    private GpuShadowCache() {
        this.resetFreeTiles();
    }

    public static GpuShadowCache get() {
        return INSTANCE;
    }

    public int getAtlasTextureId() {
        return this.atlasTextureId;
    }

    public int getSourceCapacity() {
        this.syncLayout();
        return Math.max(1, this.maxTiles / 6);
    }

    public long getRevision() {
        return this.revision;
    }

    public void release() {
        if (this.fboId != -1) {
            GL30.glDeleteFramebuffers((int)this.fboId);
            this.fboId = -1;
        }
        if (this.atlasTextureId != -1) {
            TextureUtil.releaseTextureId((int)this.atlasTextureId);
            this.atlasTextureId = -1;
        }
        if (this.shadowTileTarget != null) {
            this.shadowTileTarget.m_83930_();
            this.shadowTileTarget = null;
        }
        this.entries.clear();
        this.removedSources.clear();
        this.pendingRenderedSections.clear();
        this.expectedRenderedSections.clear();
        this.expectedRenderedSectionWaitFrames = 0;
        this.renderedSectionQuietFrames = 0;
        this.renderedSectionDeferralFrames = 0;
        this.trackedDynamicCasters.clear();
        this.trackedDynamicLevel = null;
        this.trackedDynamicCenter = null;
        this.lastDynamicScanMs = 0L;
        this.visibilityFrame = 0L;
        ++this.revision;
        this.resetFreeTiles();
    }

    public void onSourceRemoved(long sourceKey) {
        if (!GpuShadowCache.captureConsumerActive()) {
            return;
        }
        this.removedSources.add(sourceKey);
    }

    public void markAllDirty() {
        for (Entry entry : this.entries.values()) {
            GpuShadowCache.markAllFacesDirty(entry, true);
        }
        ++this.revision;
    }

    public void onSourcesRemoved(LongOpenHashSet sourceKeys) {
        if (!GpuShadowCache.captureConsumerActive() || sourceKeys == null || sourceKeys.isEmpty()) {
            return;
        }
        this.removedSources.addAll((LongCollection)sourceKeys);
    }

    public void onGeometryChanged(BlockPos pos) {
        if (!GpuShadowCache.captureConsumerActive() || pos == null || this.entries.isEmpty()) {
            return;
        }
        double px = (double)pos.m_123341_() + 0.5;
        double py = (double)pos.m_123342_() + 0.5;
        double pz = (double)pos.m_123343_() + 0.5;
        boolean changed = false;
        for (Entry entry : this.entries.values()) {
            double dx = entry.worldX - px;
            double dy = entry.worldY - py;
            double dz = entry.worldZ - pz;
            double maxDistance = (double)entry.radius + 1.25;
            if (!(dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance)) continue;
            GpuShadowCache.markAllFacesDirty(entry, true);
            changed = true;
        }
        if (changed) {
            ++this.revision;
        }
    }

    public void expectRenderedSectionChange(BlockPos pos) {
        if (!NeoGpuVanilla.isConfiguredEnabled() || pos == null) {
            return;
        }
        int originX = pos.m_123341_() >> 4 << 4;
        int originY = pos.m_123342_() >> 4 << 4;
        int originZ = pos.m_123343_() >> 4 << 4;
        boolean wasEmpty = this.expectedRenderedSections.isEmpty();
        if (this.expectedRenderedSections.add(BlockPos.m_121882_((int)originX, (int)originY, (int)originZ)) && wasEmpty) {
            this.expectedRenderedSectionWaitFrames = 0;
        }
        AABB editedBlock = new AABB(pos);
        this.markUrgentStaticEntriesAffectedByBounds(editedBlock);
    }

    public boolean onRenderedSectionChanged(int originX, int originY, int originZ) {
        if (!GpuShadowCache.captureConsumerActive()) {
            return false;
        }
        long sectionKey = BlockPos.m_121882_((int)originX, (int)originY, (int)originZ);
        if (!this.expectedRenderedSections.remove(sectionKey)) {
            return false;
        }
        if (this.expectedRenderedSections.isEmpty()) {
            this.expectedRenderedSectionWaitFrames = 0;
        }
        if (this.entries.isEmpty()) {
            return false;
        }
        boolean wasEmpty = this.pendingRenderedSections.isEmpty();
        boolean added = this.pendingRenderedSections.add(sectionKey);
        if (wasEmpty && added) {
            this.renderedSectionDeferralFrames = 0;
        }
        this.renderedSectionQuietFrames = 0;
        return added;
    }

    public boolean usesDirectEmbeddiumCapture() {
        return NeoGpuVanilla.isConfiguredEnabled() && this.embeddiumLocalShadowBridge.isAvailable();
    }

    public void refreshDynamicCasters(ClientLevel level, BlockPos center, Vec3 cameraPos) {
        boolean centerChanged;
        if (level == null || center == null || cameraPos == null) {
            return;
        }
        if (!Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) || !GpuNeoShadows.entityShadowCastersEnabled() || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) || !GpuNeoShadows.usesCubemapShadows()) {
            this.clearDynamicTracking(level != this.trackedDynamicLevel || this.trackedDynamicCenter != null);
            return;
        }
        long nowMs = System.currentTimeMillis();
        long intervalMs = this.getDynamicUpdateIntervalMs();
        boolean levelChanged = this.trackedDynamicLevel != level;
        boolean bl = centerChanged = this.trackedDynamicCenter == null || !this.trackedDynamicCenter.equals((Object)center);
        if (!levelChanged && !centerChanged && nowMs - this.lastDynamicScanMs < intervalMs) {
            return;
        }
        double scanRadius = GpuNeoShadows.getLightSearchRadius() + LightRtMath.getEntityShadowRadius(15) + 6.0;
        double scanRadiusSq = scanRadius * scanRadius;
        Long2ObjectOpenHashMap nextCasters = new Long2ObjectOpenHashMap();
        this.scanDynamicCasters(level, center, cameraPos, scanRadiusSq, (Long2ObjectOpenHashMap<AABB>)nextCasters);
        this.lastDynamicScanMs = nowMs;
        if (levelChanged) {
            for (Entry entry : this.entries.values()) {
                GpuShadowCache.markAllFacesDirty(entry, false);
            }
        } else {
            this.markDynamicCasterDiff((Long2ObjectOpenHashMap<AABB>)nextCasters);
        }
        for (AABB bounds : nextCasters.values()) {
            this.markEntriesAffectedByCasterBounds(bounds);
        }
        this.trackedDynamicCasters.clear();
        this.trackedDynamicCasters.putAll((Map)nextCasters);
        this.trackedDynamicLevel = level;
        this.trackedDynamicCenter = center.m_7949_();
    }

    public void stageVisibleEntries(ClientLevel level, long[] sourceKeys, int[] emissions, int limit) {
        int i;
        if (sourceKeys == null || emissions == null || limit <= 0) {
            return;
        }
        int max = Math.min(limit, Math.min(sourceKeys.length, emissions.length));
        if (max <= 0) {
            return;
        }
        this.ensureAtlasAndTargets();
        this.flushRemovedSources();
        long frame = ++this.visibilityFrame;
        if (NeoGpuVanilla.isConfiguredEnabled()) {
            for (Entry existing : this.entries.values()) {
                existing.stagedPriority = Integer.MAX_VALUE;
            }
        }
        LongOpenHashSet visibleKeys = new LongOpenHashSet(max);
        for (i = 0; i < max; ++i) {
            if (emissions[i] <= 0) continue;
            visibleKeys.add(sourceKeys[i]);
        }
        for (i = 0; i < max; ++i) {
            long sourceKey = sourceKeys[i];
            int emission = emissions[i];
            if (emission <= 0) continue;
            GpuLocalLightResolver.SourceGeometry geometry = GpuLocalLightResolver.resolveGeometry(level, sourceKey);
            Entry entry = (Entry)this.entries.get(sourceKey);
            if (entry == null) {
                if (this.freeTileCount < 6) {
                    this.evictLeastRecentlyVisible(visibleKeys);
                }
                if ((entry = this.createEntry(geometry, emission)) == null) continue;
                this.entries.put(sourceKey, (Object)entry);
            } else {
                boolean geometryChanged = GpuShadowCache.applyGeometry(entry, geometry);
                if (entry.emission != emission) {
                    entry.emission = emission;
                    entry.radius = (float)LightRtMath.getBlockShadowRadius(emission);
                    geometryChanged = true;
                }
                if (geometryChanged) {
                    GpuShadowCache.markAllFacesDirty(entry, true);
                }
            }
            entry.lastVisibleFrame = frame;
            entry.stagedPriority = i;
            ++entry.framesAlive;
            if (NeoGpuVanilla.isConfiguredEnabled() || GpuNeoShadows.isHardShadowBakeEnabled()) continue;
            if (entry.refreshCount < 8) {
                if (entry.framesAlive != 10 && entry.framesAlive != 30 && entry.framesAlive != 60 && entry.framesAlive != 120 && entry.framesAlive != 240 && entry.framesAlive != 480 && entry.framesAlive != 900) continue;
                GpuShadowCache.markAllFacesDirty(entry, true);
                ++entry.refreshCount;
                continue;
            }
            if (entry.framesAlive % 600 != 0) continue;
            GpuShadowCache.markAllFacesDirty(entry, true);
        }
    }

    public int collectVisibleEntries(long[] sourceKeys, int[] emissions, int limit, double cameraX, double cameraY, double cameraZ, float[] lightPosRadiusOut, float[] atlasRectOut, long[] visibleSourceKeysOut, int[] visibleEmissionsOut) {
        return this.collectVisibleEntries(sourceKeys, emissions, limit, cameraX, cameraY, cameraZ, lightPosRadiusOut, atlasRectOut, visibleSourceKeysOut, visibleEmissionsOut, null);
    }

    public int collectVisibleEntries(long[] sourceKeys, int[] emissions, int limit, double cameraX, double cameraY, double cameraZ, float[] lightPosRadiusOut, float[] atlasRectOut, long[] visibleSourceKeysOut, int[] visibleEmissionsOut, float[] geometryOut) {
        if (sourceKeys == null || emissions == null || limit <= 0) {
            return 0;
        }
        int count = 0;
        int max = Math.min(limit, Math.min(sourceKeys.length, emissions.length));
        for (int i = 0; i < max; ++i) {
            Entry entry;
            if (emissions[i] <= 0 || (entry = (Entry)this.entries.get(sourceKeys[i])) == null || !entry.ready || GpuShadowCache.hasDirtyFaces(entry)) continue;
            int posBase = count * 4;
            lightPosRadiusOut[posBase] = (float)(entry.worldX - cameraX);
            lightPosRadiusOut[posBase + 1] = (float)(entry.worldY - cameraY);
            lightPosRadiusOut[posBase + 2] = (float)(entry.worldZ - cameraZ);
            lightPosRadiusOut[posBase + 3] = entry.radius;
            if (geometryOut != null && posBase + 3 < geometryOut.length) {
                geometryOut[posBase] = (float)(entry.blockCenterX - cameraX);
                geometryOut[posBase + 1] = (float)(entry.blockCenterY - cameraY);
                geometryOut[posBase + 2] = (float)(entry.blockCenterZ - cameraZ);
                geometryOut[posBase + 3] = (float)entry.blockedFaceMask + (entry.boxEmitter ? 64.0f : 0.0f);
            }
            int rectBase = count * 24;
            for (int face = 0; face < 6; ++face) {
                atlasRectOut[rectBase + face * 4] = entry.u0[face];
                atlasRectOut[rectBase + face * 4 + 1] = entry.v0[face];
                atlasRectOut[rectBase + face * 4 + 2] = entry.us;
                atlasRectOut[rectBase + face * 4 + 3] = entry.vs;
            }
            if (visibleSourceKeysOut != null && count < visibleSourceKeysOut.length) {
                visibleSourceKeysOut[count] = sourceKeys[i];
            }
            if (visibleEmissionsOut != null && count < visibleEmissionsOut.length) {
                visibleEmissionsOut[count] = emissions[i];
            }
            ++count;
        }
        return count;
    }

    public int collectNeoGpuVanillaEntries(long[] sourceKeys, int[] emissions, int limit, double referenceX, double referenceY, double referenceZ, float[] lightPosRadiusOut, float[] atlasRectOut, long[] visibleSourceKeysOut, int[] visibleEmissionsOut, float[] geometryOut) {
        if (sourceKeys == null || emissions == null || limit <= 0) {
            return 0;
        }
        int count = 0;
        int max = Math.min(limit, Math.min(sourceKeys.length, emissions.length));
        for (int i = 0; i < max; ++i) {
            Entry entry;
            int emission = emissions[i];
            if (emission <= 0 || (entry = (Entry)this.entries.get(sourceKeys[i])) == null) continue;
            boolean capturePending = !entry.ready || GpuShadowCache.hasDirtyFaces(entry);
            int posBase = count * 4;
            lightPosRadiusOut[posBase] = (float)(entry.worldX - referenceX);
            lightPosRadiusOut[posBase + 1] = (float)(entry.worldY - referenceY);
            lightPosRadiusOut[posBase + 2] = (float)(entry.worldZ - referenceZ);
            lightPosRadiusOut[posBase + 3] = entry.radius;
            if (geometryOut != null && posBase + 3 < geometryOut.length) {
                geometryOut[posBase] = (float)(entry.blockCenterX - referenceX);
                geometryOut[posBase + 1] = (float)(entry.blockCenterY - referenceY);
                geometryOut[posBase + 2] = (float)(entry.blockCenterZ - referenceZ);
                int flags = entry.blockedFaceMask & 0x3F;
                if (entry.boxEmitter) {
                    flags |= 0x40;
                }
                if (capturePending) {
                    flags |= 0x80;
                }
                geometryOut[posBase + 3] = flags;
            }
            int rectBase = count * 24;
            for (int face = 0; face < 6; ++face) {
                atlasRectOut[rectBase + face * 4] = entry.u0[face];
                atlasRectOut[rectBase + face * 4 + 1] = entry.v0[face];
                atlasRectOut[rectBase + face * 4 + 2] = entry.us;
                atlasRectOut[rectBase + face * 4 + 3] = entry.vs;
            }
            if (visibleSourceKeysOut != null && count < visibleSourceKeysOut.length) {
                visibleSourceKeysOut[count] = sourceKeys[i];
            }
            if (visibleEmissionsOut != null && count < visibleEmissionsOut.length) {
                visibleEmissionsOut[count] = emission;
            }
            ++count;
        }
        return count;
    }

    public long getNeoGpuVanillaContentGeneration(long[] sourceKeys, int limit) {
        if (sourceKeys == null || limit <= 0) {
            return 0L;
        }
        int max = Math.min(limit, sourceKeys.length);
        long xor = 0L;
        long sum = 0L;
        for (int i = 0; i < max; ++i) {
            long key = sourceKeys[i];
            Entry entry = (Entry)this.entries.get(key);
            long generation = entry == null ? -1L : entry.neoGpuContentGeneration;
            long value = key ^ generation * -7046029254386353131L;
            value ^= value >>> 30;
            value *= -4658895280553007687L;
            value ^= value >>> 27;
            value *= -7723592293110705685L;
            value ^= value >>> 31;
            xor ^= value;
            sum += Long.rotateLeft(value, (int)(key & 0x3FL));
        }
        return xor ^ Long.rotateLeft(sum, max & 0x3F) ^ (long)max * -4417276706812531889L;
    }

    public boolean hasPendingNeoGpuVanillaCaptures(long[] sourceKeys, int limit) {
        if (!this.expectedRenderedSections.isEmpty() || !this.pendingRenderedSections.isEmpty()) {
            return true;
        }
        if (sourceKeys == null || limit <= 0) {
            return false;
        }
        int max = Math.min(limit, sourceKeys.length);
        for (int i = 0; i < max; ++i) {
            Entry entry = (Entry)this.entries.get(sourceKeys[i]);
            if (entry != null && entry.ready && !GpuShadowCache.hasDirtyFaces(entry)) continue;
            return true;
        }
        return false;
    }

    public int countDirtyFacesForSources(long[] sourceKeys, int limit) {
        if (sourceKeys == null || limit <= 0) {
            return 0;
        }
        int count = 0;
        int max = Math.min(limit, sourceKeys.length);
        for (int i = 0; i < max; ++i) {
            Entry entry = (Entry)this.entries.get(sourceKeys[i]);
            if (entry == null || !entry.ready) {
                count += 6;
                continue;
            }
            count += Integer.bitCount(entry.dirtyFaceMask & 0x3F);
        }
        return count;
    }

    public int countPendingNeoGpuVanillaFaces(long[] sourceKeys, int limit) {
        if (sourceKeys == null || limit <= 0) {
            return 0;
        }
        int count = 0;
        int max = Math.min(limit, sourceKeys.length);
        for (int i = 0; i < max; ++i) {
            Entry entry = (Entry)this.entries.get(sourceKeys[i]);
            if (entry == null) {
                count += 6;
                continue;
            }
            count += Integer.bitCount(entry.dirtyFaceMask & 0x3F);
        }
        return count;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void buildPendingShadows(LevelRenderer levelRenderer, float partialTick, long finishNano, GameRenderer gameRenderer, LightTexture lightTexture) {
        block23: {
            if (!GpuShadowCache.captureConsumerActive() || this.entries.isEmpty()) {
                return;
            }
            this.flushExpectedRenderedSectionsIfTimedOut();
            this.flushRenderedSectionInvalidationsIfReady();
            boolean urgentMeshUploadPending = !this.expectedRenderedSections.isEmpty();
            boolean hasDirty = false;
            for (Entry e : this.entries.values()) {
                if (!GpuShadowCache.hasDirtyFaces(e)) continue;
                hasDirty = true;
                break;
            }
            if (!hasDirty) {
                return;
            }
            Minecraft mc = Minecraft.m_91087_();
            ClientLevel level = mc.f_91073_;
            if (level == null) {
                return;
            }
            RenderTarget oldMainTarget = mc.m_91385_();
            Matrix4f oldProj = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
            VertexSorting oldSorting = RenderSystem.getVertexSorting();
            this.ensureAtlasAndTargets();
            this.shadowCamera.m_90575_((BlockGetter)level, (Entity)(mc.m_91288_() != null ? mc.m_91288_() : mc.f_91074_), true, false, partialTick);
            ((MinecraftAccessor)mc).luxium$setMainRenderTargetField(this.shadowTileTarget);
            NeoGpuVanillaGpuDebug.beginCapture();
            try {
                int dynamicFacesThisFrame = 0;
                if (NeoGpuVanilla.isConfiguredEnabled()) {
                    int faceBudget = this.neoGpuFaceRebuildBudget();
                    for (staticBuildsThisFrame = 0; staticBuildsThisFrame < faceBudget; ++staticBuildsThisFrame) {
                        int face;
                        nearestDirty = null;
                        for (Entry entry : this.entries.values()) {
                            if (entry.lastVisibleFrame != this.visibilityFrame || !GpuShadowCache.hasDirtyFaces(entry) || !entry.staticDirty || urgentMeshUploadPending && entry.urgentStaticDirty || nearestDirty != null && (!entry.urgentStaticDirty || nearestDirty.urgentStaticDirty) && (entry.urgentStaticDirty != nearestDirty.urgentStaticDirty || entry.stagedPriority >= nearestDirty.stagedPriority)) continue;
                            nearestDirty = entry;
                        }
                        if (nearestDirty != null && (face = Integer.numberOfTrailingZeros(nearestDirty.dirtyFaceMask & 0x3F)) >= 0 && face < 6) {
                            this.rebuildFaces(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture, nearestDirty, 1 << face);
                            continue;
                        }
                        break;
                    }
                } else if (GpuNeoShadows.isHardShadowBakeEnabled()) {
                    int faceBudget = Math.max(1, (Integer)Config.CLIENT.gpuHardShadowCaptureBudget.get());
                    while (staticBuildsThisFrame < faceBudget) {
                        int face;
                        nearestDirty = null;
                        for (Entry entry : this.entries.values()) {
                            if (entry.lastVisibleFrame != this.visibilityFrame || !GpuShadowCache.hasDirtyFaces(entry) || !entry.staticDirty || nearestDirty != null && (!entry.urgentStaticDirty || nearestDirty.urgentStaticDirty) && (entry.urgentStaticDirty != nearestDirty.urgentStaticDirty || entry.stagedPriority >= nearestDirty.stagedPriority)) continue;
                            nearestDirty = entry;
                        }
                        if (nearestDirty != null && (face = Integer.numberOfTrailingZeros(nearestDirty.dirtyFaceMask & 0x3F)) >= 0 && face < 6) {
                            this.rebuildFaces(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture, nearestDirty, 1 << face);
                            ++staticBuildsThisFrame;
                            continue;
                        }
                        break;
                    }
                } else {
                    for (Entry entry : this.entries.values()) {
                        if (staticBuildsThisFrame < 1) {
                            if (!GpuShadowCache.hasDirtyFaces(entry) || !entry.staticDirty) continue;
                            this.rebuildFaces(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture, entry, entry.dirtyFaceMask);
                            ++staticBuildsThisFrame;
                            continue;
                        }
                        break;
                    }
                }
                if (dynamicFacesThisFrame >= 24) break block23;
                for (Entry entry : this.entries.values()) {
                    int singleFaceMask;
                    if (dynamicFacesThisFrame >= 24) {
                        break;
                    }
                    if (!GpuShadowCache.hasDirtyFaces(entry) || entry.staticDirty) continue;
                    for (int mask = entry.dirtyFaceMask; mask != 0 && dynamicFacesThisFrame < 24; mask &= ~singleFaceMask, ++dynamicFacesThisFrame) {
                        int face = Integer.numberOfTrailingZeros(mask);
                        singleFaceMask = 1 << face;
                        this.rebuildFaces(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture, entry, singleFaceMask);
                    }
                }
            }
            finally {
                NeoGpuVanillaGpuDebug.endCapture();
                NeoShadowsEngine.endGpuShadowAtlasCapture();
                ((MinecraftAccessor)mc).luxium$setMainRenderTargetField(oldMainTarget);
                RenderSystem.setProjectionMatrix((Matrix4f)oldProj, (VertexSorting)oldSorting);
                oldMainTarget.m_83947_(true);
                GL11.glDisable((int)3089);
                GlStateManager._activeTexture((int)33984);
                GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
                GlStateManager._depthMask((boolean)true);
                GlStateManager._enableDepthTest();
                RenderSystem.depthFunc((int)515);
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
                GL11.glCullFace((int)1029);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void rebuildFaces(LevelRenderer levelRenderer, float partialTick, long finishNano, GameRenderer gameRenderer, LightTexture lightTexture, Entry entry, int faceMask) {
        EmbeddiumLocalShadowBridge.PreparedCapture directCapture;
        int mask = faceMask & 0x3F;
        if (mask == 0) {
            return;
        }
        boolean directEmbeddium = NeoGpuVanilla.isConfiguredEnabled() && this.embeddiumLocalShadowBridge.isAvailable();
        EmbeddiumLocalShadowBridge.PreparedCapture preparedCapture = directCapture = directEmbeddium ? this.embeddiumLocalShadowBridge.prepare(entry.worldX, entry.worldY, entry.worldZ, entry.radius) : null;
        if (directEmbeddium && (directCapture == null || directCapture.lists().regionCount() == 0)) {
            return;
        }
        while (mask != 0) {
            int face = Integer.numberOfTrailingZeros(mask);
            int singleFaceMask = 1 << face;
            this.shadowTileTarget.m_83954_(Minecraft.f_91002_);
            this.shadowTileTarget.m_83947_(true);
            ((CameraAccessor)this.shadowCamera).luxium$setPosition(entry.worldX, entry.worldY, entry.worldZ);
            ((CameraAccessor)this.shadowCamera).luxium$setRotation(GpuShadowCache.getYawForFace(face), GpuShadowCache.getPitchForFace(face));
            Matrix4f proj = new Matrix4f().setPerspective(SHADOW_FACE_FOV, 1.0f, 0.55f, entry.radius);
            RenderSystem.setProjectionMatrix((Matrix4f)proj, (VertexSorting)VertexSorting.f_276450_);
            NeoShadowsEngine.beginGpuShadowAtlasCapture(entry.worldX, entry.worldY, entry.worldZ, entry.radius, face, GpuNeoShadows.entityShadowCastersEnabled() && GpuNeoShadows.isActive());
            try {
                boolean renderedDirect;
                PoseStack shadowPose = new PoseStack();
                shadowPose.m_252781_(Axis.f_252529_.m_252977_(this.shadowCamera.m_90589_()));
                shadowPose.m_252781_(Axis.f_252436_.m_252977_(this.shadowCamera.m_90590_() + 180.0f));
                Matrix4f lightViewRotation = new Matrix4f((Matrix4fc)shadowPose.m_85850_().m_252922_());
                boolean bl = renderedDirect = directEmbeddium && this.embeddiumLocalShadowBridge.renderFace(directCapture, proj, lightViewRotation);
                if (directEmbeddium && !renderedDirect) {
                    return;
                }
                if (!directEmbeddium) {
                    levelRenderer.m_109599_(shadowPose, partialTick, finishNano, false, this.shadowCamera, gameRenderer, lightTexture, proj);
                }
                if (!this.copyDepthToAtlas(entry, face, proj)) {
                    mask &= ~singleFaceMask;
                    continue;
                }
            }
            finally {
                NeoShadowsEngine.endGpuShadowAtlasCapture();
                continue;
            }
            entry.validFaceMask |= singleFaceMask;
            entry.dirtyFaceMask &= ~singleFaceMask;
            ++entry.neoGpuContentGeneration;
            NeoGpuVanillaGpuDebug.recordCapturedFace();
            mask &= ~singleFaceMask;
        }
        entry.ready = entry.validFaceMask == 63;
        ++this.revision;
        if (entry.dirtyFaceMask == 0) {
            entry.staticDirty = false;
            entry.dynamicDirty = false;
            entry.urgentStaticDirty = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean copyDepthToAtlas(Entry entry, int face, Matrix4f proj) {
        ShaderInstance shader = ShaderManager.getDepthToDistanceShader();
        if (shader == null) {
            return false;
        }
        GL30.glBindFramebuffer((int)36160, (int)this.fboId);
        int tileX = entry.tileIndices[face] % this.tileGrid;
        int tileY = entry.tileIndices[face] / this.tileGrid;
        int vx = tileX * this.tileSize;
        int vy = tileY * this.tileSize;
        GL11.glViewport((int)vx, (int)vy, (int)this.tileSize, (int)this.tileSize);
        GL11.glEnable((int)3089);
        GL11.glScissor((int)vx, (int)vy, (int)this.tileSize, (int)this.tileSize);
        GL11.glDisable((int)2929);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        try {
            shader.m_173350_("DepthSampler", (Object)this.shadowTileTarget.m_83980_());
            if (shader.m_173348_("InverseProj") != null) {
                shader.m_173348_("InverseProj").m_5679_(new Matrix4f((Matrix4fc)proj).invert());
            }
            if (shader.m_173348_("LightRadius") != null) {
                shader.m_173348_("LightRadius").m_5985_(entry.radius);
            }
            Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)this.tileSize, (float)this.tileSize, 0.0f, -1.0f, 1.0f);
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            shader.f_173308_.m_5679_(new Matrix4f());
            shader.f_173309_.m_5679_(ortho);
            shader.m_173363_();
            Tesselator tess = RenderSystem.renderThreadTesselator();
            BufferBuilder buf = tess.m_85915_();
            buf.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buf.m_5483_(0.0, (double)this.tileSize, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buf.m_5483_((double)this.tileSize, (double)this.tileSize, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buf.m_5483_((double)this.tileSize, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buf.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buf.m_231175_());
            boolean bl = true;
            return bl;
        }
        finally {
            shader.m_173362_();
            GL11.glDisable((int)3089);
            GL11.glEnable((int)2929);
            GL30.glBindFramebuffer((int)36160, (int)0);
        }
    }

    private static float getYawForFace(int face) {
        return switch (face) {
            case 0 -> -90.0f;
            case 1 -> 90.0f;
            case 4 -> 0.0f;
            case 5 -> 180.0f;
            default -> 0.0f;
        };
    }

    private static float getPitchForFace(int face) {
        return switch (face) {
            case 2 -> -90.0f;
            case 3 -> 90.0f;
            default -> 0.0f;
        };
    }

    private void ensureAtlasAndTargets() {
        this.syncLayout();
        if (this.atlasTextureId == -1) {
            this.atlasTextureId = TextureUtil.generateTextureId();
            RenderSystem.activeTexture((int)33990);
            GL11.glBindTexture((int)3553, (int)this.atlasTextureId);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
            GL11.glTexImage2D((int)3553, (int)0, (int)33326, (int)4096, (int)4096, (int)0, (int)6403, (int)5126, (ByteBuffer)null);
            this.fboId = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer((int)36160, (int)this.fboId);
            GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)this.atlasTextureId, (int)0);
            GL30.glBindFramebuffer((int)36160, (int)0);
            RenderSystem.activeTexture((int)33984);
        }
        if (this.shadowTileTarget == null) {
            this.shadowTileTarget = new TextureTarget(this.tileSize, this.tileSize, true, Minecraft.f_91002_);
            this.shadowTileTarget.m_83931_(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void flushRemovedSources() {
        if (this.removedSources.isEmpty()) {
            return;
        }
        LongIterator it = this.removedSources.iterator();
        while (it.hasNext()) {
            long sourceKey = it.nextLong();
            Entry removed = (Entry)this.entries.remove(sourceKey);
            this.releaseEntryTiles(removed);
        }
        this.removedSources.clear();
        ++this.revision;
    }

    private void evictLeastRecentlyVisible(LongOpenHashSet protectedKeys) {
        long victimKey = 0L;
        Entry victim = null;
        LongIterator longIterator = this.entries.keySet().iterator();
        while (longIterator.hasNext()) {
            Entry candidate;
            long key = (Long)longIterator.next();
            if (protectedKeys.contains(key) || (candidate = (Entry)this.entries.get(key)) == null || victim != null && candidate.lastVisibleFrame >= victim.lastVisibleFrame) continue;
            victimKey = key;
            victim = candidate;
        }
        if (victim != null) {
            this.entries.remove(victimKey);
            this.releaseEntryTiles(victim);
        }
    }

    private void releaseEntryTiles(@Nullable Entry entry) {
        if (entry == null) {
            return;
        }
        for (int face = 0; face < 6; ++face) {
            if (this.freeTileCount >= this.freeTiles.length) continue;
            this.freeTiles[this.freeTileCount++] = entry.tileIndices[face];
        }
    }

    @Nullable
    private Entry createEntry(GpuLocalLightResolver.SourceGeometry geometry, int emission) {
        if (this.freeTileCount < 6) {
            return null;
        }
        Entry entry = new Entry();
        entry.emission = emission;
        entry.radius = (float)LightRtMath.getBlockShadowRadius(emission);
        for (int f = 0; f < 6; ++f) {
            int tileIndex;
            entry.tileIndices[f] = tileIndex = this.freeTiles[--this.freeTileCount];
            int tileX = tileIndex % this.tileGrid;
            int tileY = tileIndex / this.tileGrid;
            entry.u0[f] = ((float)(tileX * this.tileSize) + 0.5f) / 4096.0f;
            entry.v0[f] = ((float)(tileY * this.tileSize) + 0.5f) / 4096.0f;
        }
        entry.us = ((float)this.tileSize - 1.0f) / 4096.0f;
        entry.vs = ((float)this.tileSize - 1.0f) / 4096.0f;
        GpuShadowCache.applyGeometry(entry, geometry);
        entry.dirtyFaceMask = 63;
        entry.staticDirty = true;
        return entry;
    }

    private static boolean applyGeometry(Entry entry, GpuLocalLightResolver.SourceGeometry geometry) {
        boolean changed = Double.compare(entry.worldX, geometry.sourceX) != 0 || Double.compare(entry.worldY, geometry.sourceY) != 0 || Double.compare(entry.worldZ, geometry.sourceZ) != 0 || Double.compare(entry.blockCenterX, geometry.centerX) != 0 || Double.compare(entry.blockCenterY, geometry.centerY) != 0 || Double.compare(entry.blockCenterZ, geometry.centerZ) != 0 || entry.boxEmitter != geometry.boxEmitter || entry.blockedFaceMask != geometry.blockedFaceMask;
        entry.worldX = geometry.sourceX;
        entry.worldY = geometry.sourceY;
        entry.worldZ = geometry.sourceZ;
        entry.blockCenterX = geometry.centerX;
        entry.blockCenterY = geometry.centerY;
        entry.blockCenterZ = geometry.centerZ;
        entry.boxEmitter = geometry.boxEmitter;
        entry.blockedFaceMask = geometry.blockedFaceMask;
        return changed;
    }

    private static boolean hasDirtyFaces(Entry entry) {
        return entry != null && (entry.dirtyFaceMask & 0x3F) != 0;
    }

    private static void markAllFacesDirty(Entry entry, boolean staticDirty) {
        if (entry == null) {
            return;
        }
        entry.dirtyFaceMask |= 0x3F;
        if (staticDirty) {
            entry.staticDirty = true;
        } else {
            entry.dynamicDirty = true;
        }
    }

    private void markDynamicCasterDiff(Long2ObjectOpenHashMap<AABB> nextCasters) {
        long key;
        LongIterator longIterator = this.trackedDynamicCasters.keySet().iterator();
        while (longIterator.hasNext()) {
            key = (Long)longIterator.next();
            AABB oldBox = (AABB)this.trackedDynamicCasters.get(key);
            AABB newBox = (AABB)nextCasters.get(key);
            if (newBox == null) {
                this.markEntriesAffectedByCasterBounds(oldBox);
                continue;
            }
            if (GpuShadowCache.sameBounds(oldBox, newBox)) continue;
            this.markEntriesAffectedByCasterBounds(GpuShadowCache.union(oldBox, newBox));
        }
        longIterator = nextCasters.keySet().iterator();
        while (longIterator.hasNext()) {
            key = (Long)longIterator.next();
            if (this.trackedDynamicCasters.containsKey(key)) continue;
            this.markEntriesAffectedByCasterBounds((AABB)nextCasters.get(key));
        }
    }

    private void markEntriesAffectedByCasterBounds(@Nullable AABB bounds) {
        if (bounds == null || this.entries.isEmpty()) {
            return;
        }
        for (Entry entry : this.entries.values()) {
            double influence = Math.max((double)entry.radius, LightRtMath.getEntityShadowRadius(entry.emission)) + 1.25;
            if (!GpuShadowCache.isBoundsNearLight(bounds, entry.worldX, entry.worldY, entry.worldZ, influence)) continue;
            int faceMask = GpuShadowCache.computeAffectedFaceMask(entry, bounds);
            if (faceMask == 0) {
                faceMask = 63;
            }
            entry.dirtyFaceMask |= faceMask;
            entry.dynamicDirty = true;
        }
    }

    private void markUrgentStaticEntriesAffectedByBounds(@Nullable AABB bounds) {
        if (bounds == null || this.entries.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Entry entry : this.entries.values()) {
            double influence = (double)entry.radius + 1.25;
            if (!GpuShadowCache.isBoundsNearLight(bounds, entry.worldX, entry.worldY, entry.worldZ, influence)) continue;
            int faceMask = GpuShadowCache.computeAffectedFaceMask(entry, bounds);
            if (faceMask == 0) {
                faceMask = 63;
            }
            int newlyDirty = faceMask & ~entry.dirtyFaceMask;
            entry.dirtyFaceMask |= faceMask;
            entry.staticDirty = true;
            entry.urgentStaticDirty = true;
            if (newlyDirty == 0) continue;
            changed = true;
        }
        if (changed) {
            ++this.revision;
        }
    }

    private static boolean isBoundsNearLight(AABB bounds, double lightX, double lightY, double lightZ, double influence) {
        double cz;
        double dz;
        double cy;
        double dy;
        double cx = GpuShadowCache.clamp(lightX, bounds.f_82288_, bounds.f_82291_);
        double dx = lightX - cx;
        return dx * dx + (dy = lightY - (cy = GpuShadowCache.clamp(lightY, bounds.f_82289_, bounds.f_82292_))) * dy + (dz = lightZ - (cz = GpuShadowCache.clamp(lightZ, bounds.f_82290_, bounds.f_82293_))) * dz <= influence * influence;
    }

    private static int computeAffectedFaceMask(Entry entry, AABB bounds) {
        int mask = 0;
        for (double x : new double[]{bounds.f_82288_, bounds.f_82291_}) {
            for (double y : new double[]{bounds.f_82289_, bounds.f_82292_}) {
                for (double z : new double[]{bounds.f_82290_, bounds.f_82293_}) {
                    mask |= GpuShadowCache.faceMaskForDirection(x - entry.worldX, y - entry.worldY, z - entry.worldZ);
                }
            }
        }
        return mask;
    }

    private static int faceMaskForDirection(double dx, double dy, double dz) {
        double absZ;
        double absY;
        double absX = Math.abs(dx);
        double maxAxis = Math.max(absX, Math.max(absY = Math.abs(dy), absZ = Math.abs(dz)));
        if (maxAxis <= 1.0E-6) {
            return 63;
        }
        int mask = 0;
        double epsilon = maxAxis * 0.12;
        if (absX + epsilon >= maxAxis) {
            mask |= dx >= 0.0 ? 1 : 2;
        }
        if (absY + epsilon >= maxAxis) {
            mask |= dy >= 0.0 ? 4 : 8;
        }
        if (absZ + epsilon >= maxAxis) {
            mask |= dz >= 0.0 ? 16 : 32;
        }
        return mask == 0 ? 63 : mask;
    }

    private long getDynamicUpdateIntervalMs() {
        int fps = Math.max(1, (Integer)Config.CLIENT.entityShadowUpdateFpsLimit.get());
        return Math.max(1L, 1000L / (long)fps);
    }

    private void clearDynamicTracking(boolean markDirty) {
        if (markDirty) {
            for (Entry entry : this.entries.values()) {
                if (!entry.dynamicDirty) continue;
                entry.dynamicDirty = false;
            }
        }
        this.trackedDynamicCasters.clear();
        this.trackedDynamicLevel = null;
        this.trackedDynamicCenter = null;
        this.lastDynamicScanMs = 0L;
    }

    private void scanDynamicCasters(ClientLevel level, BlockPos center, Vec3 cameraPos, double radiusSq, Long2ObjectOpenHashMap<AABB> out) {
        Vec3 centerPos = Vec3.m_82512_((Vec3i)center);
        for (Entity entity : level.m_104735_()) {
            AABB box;
            if (entity == null || entity.m_213877_() || entity.m_20145_() || (box = entity.m_20191_()) == null || GpuShadowCache.distanceToAabbSq(centerPos, box) > radiusSq) continue;
            out.put((long)entity.m_19879_(), (Object)box);
        }
        BlockEntityRenderDispatcher dispatcher = Minecraft.m_91087_().m_167982_();
        int chunkRadius = Math.max(1, (int)Math.ceil(Math.sqrt(radiusSq) / 16.0));
        int centerChunkX = center.m_123341_() >> 4;
        int centerChunkZ = center.m_123343_() >> 4;
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                LevelChunk chunk = level.m_7726_().m_7131_(centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                    BlockEntityRenderer renderer;
                    if (blockEntity == null || blockEntity.m_58901_() || (renderer = dispatcher.m_112265_(blockEntity)) == null || !renderer.m_142756_(blockEntity, cameraPos)) continue;
                    AABB box = blockEntity.getRenderBoundingBox();
                    if (box == null) {
                        box = new AABB(blockEntity.m_58899_());
                    }
                    if (GpuShadowCache.distanceToAabbSq(centerPos, box) > radiusSq) continue;
                    out.put(GpuShadowCache.blockEntityKey(blockEntity.m_58899_().m_121878_()), (Object)box);
                }
            }
        }
    }

    private static boolean sameBounds(@Nullable AABB a, @Nullable AABB b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Double.compare(a.f_82288_, b.f_82288_) == 0 && Double.compare(a.f_82289_, b.f_82289_) == 0 && Double.compare(a.f_82290_, b.f_82290_) == 0 && Double.compare(a.f_82291_, b.f_82291_) == 0 && Double.compare(a.f_82292_, b.f_82292_) == 0 && Double.compare(a.f_82293_, b.f_82293_) == 0;
    }

    private static AABB union(AABB a, AABB b) {
        return new AABB(Math.min(a.f_82288_, b.f_82288_), Math.min(a.f_82289_, b.f_82289_), Math.min(a.f_82290_, b.f_82290_), Math.max(a.f_82291_, b.f_82291_), Math.max(a.f_82292_, b.f_82292_), Math.max(a.f_82293_, b.f_82293_));
    }

    private static double distanceToAabbSq(Vec3 point, AABB box) {
        double dx = point.f_82479_ < box.f_82288_ ? box.f_82288_ - point.f_82479_ : Math.max(0.0, point.f_82479_ - box.f_82291_);
        double dy = point.f_82480_ < box.f_82289_ ? box.f_82289_ - point.f_82480_ : Math.max(0.0, point.f_82480_ - box.f_82292_);
        double dz = point.f_82481_ < box.f_82290_ ? box.f_82290_ - point.f_82481_ : Math.max(0.0, point.f_82481_ - box.f_82293_);
        return dx * dx + dy * dy + dz * dz;
    }

    private static long blockEntityKey(long posKey) {
        return posKey ^ Long.MIN_VALUE;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void flushExpectedRenderedSectionsIfTimedOut() {
        if (this.expectedRenderedSections.isEmpty()) {
            return;
        }
        ++this.expectedRenderedSectionWaitFrames;
        if (this.expectedRenderedSectionWaitFrames < 20) {
            return;
        }
        boolean wasEmpty = this.pendingRenderedSections.isEmpty();
        this.pendingRenderedSections.addAll((LongCollection)this.expectedRenderedSections);
        this.expectedRenderedSections.clear();
        this.expectedRenderedSectionWaitFrames = 0;
        if (wasEmpty && !this.pendingRenderedSections.isEmpty()) {
            this.renderedSectionDeferralFrames = 0;
        }
        this.renderedSectionQuietFrames = 0;
    }

    private void flushRenderedSectionInvalidationsIfReady() {
        if (this.pendingRenderedSections.isEmpty()) {
            return;
        }
        ++this.renderedSectionDeferralFrames;
        if (this.renderedSectionQuietFrames > 0) {
            --this.renderedSectionQuietFrames;
        }
        if (this.renderedSectionQuietFrames > 0) {
            return;
        }
        boolean changed = false;
        LongIterator longIterator = this.pendingRenderedSections.iterator();
        while (longIterator.hasNext()) {
            long sectionKey = (Long)longIterator.next();
            int originX = BlockPos.m_121983_((long)sectionKey);
            int originY = BlockPos.m_122008_((long)sectionKey);
            int originZ = BlockPos.m_122015_((long)sectionKey);
            AABB bounds = new AABB((double)originX, (double)originY, (double)originZ, (double)originX + 16.0, (double)originY + 16.0, (double)originZ + 16.0);
            for (Entry entry : this.entries.values()) {
                double influence = (double)entry.radius + 1.25;
                if (!GpuShadowCache.isBoundsNearLight(bounds, entry.worldX, entry.worldY, entry.worldZ, influence)) continue;
                int faceMask = GpuShadowCache.computeAffectedFaceMask(entry, bounds);
                if (faceMask == 0) {
                    faceMask = 63;
                }
                int newlyDirty = faceMask & ~entry.dirtyFaceMask;
                entry.dirtyFaceMask |= faceMask;
                entry.staticDirty = true;
                entry.urgentStaticDirty = true;
                if (newlyDirty == 0) continue;
                changed = true;
            }
        }
        this.pendingRenderedSections.clear();
        this.renderedSectionQuietFrames = 0;
        this.renderedSectionDeferralFrames = 0;
        if (changed) {
            ++this.revision;
        }
    }

    private static boolean captureConsumerActive() {
        return GpuNeoShadows.isActive() && GpuNeoShadows.usesCubemapShadows() || NeoGpuVanilla.isConfiguredEnabled();
    }

    private int neoGpuFaceRebuildBudget() {
        return Math.max(1, (Integer)Config.CLIENT.neoGpuVanillaCaptureBudget.get());
    }

    private void syncLayout() {
        int requested = NeoGpuVanilla.isConfiguredEnabled() ? GpuShadowCache.neoGpuVanillaCapturePixels((Integer)Config.CLIENT.neoGpuVanillaCaptureResolution.get(), Math.max(1, Math.min(40, (Integer)Config.CLIENT.neoGpuVanillaMaxSources.get()))) : 256;
        if ((requested = GpuShadowCache.nearestPowerOfTwo(Math.max(16, Math.min(512, requested)))) == this.tileSize) {
            return;
        }
        if (this.fboId != -1) {
            GL30.glDeleteFramebuffers((int)this.fboId);
            this.fboId = -1;
        }
        if (this.atlasTextureId != -1) {
            TextureUtil.releaseTextureId((int)this.atlasTextureId);
            this.atlasTextureId = -1;
        }
        if (this.shadowTileTarget != null) {
            this.shadowTileTarget.m_83930_();
            this.shadowTileTarget = null;
        }
        this.entries.clear();
        this.removedSources.clear();
        this.pendingRenderedSections.clear();
        this.expectedRenderedSections.clear();
        this.expectedRenderedSectionWaitFrames = 0;
        this.renderedSectionQuietFrames = 0;
        this.renderedSectionDeferralFrames = 0;
        this.tileSize = requested;
        this.tileGrid = Math.max(1, 4096 / this.tileSize);
        this.maxTiles = this.tileGrid * this.tileGrid;
        this.freeTiles = new int[this.maxTiles];
        this.resetFreeTiles();
        ++this.revision;
    }

    private static int neoGpuVanillaCapturePixels(int pixelsPerBlock, int requestedSources) {
        int grid;
        int capacity;
        int p = Math.max(1, pixelsPerBlock);
        int capturePixels = p >= 32 ? 512 : 16 * p;
        int wanted = Math.max(1, Math.min(40, requestedSources));
        for (capturePixels = GpuShadowCache.nearestPowerOfTwo(Math.max(16, Math.min(512, capturePixels))); capturePixels > 16 && (capacity = (grid = Math.max(1, 4096 / capturePixels)) * grid / 6) < wanted; capturePixels >>= 1) {
        }
        return capturePixels;
    }

    private static int nearestPowerOfTwo(int value) {
        int[] values = new int[]{16, 32, 64, 128, 256, 512};
        int best = values[0];
        int bestDistance = Math.abs(value - best);
        for (int candidate : values) {
            int distance = Math.abs(value - candidate);
            if (distance >= bestDistance) continue;
            best = candidate;
            bestDistance = distance;
        }
        return best;
    }

    private void resetFreeTiles() {
        this.freeTileCount = this.maxTiles;
        if (this.freeTiles.length != this.maxTiles) {
            this.freeTiles = new int[this.maxTiles];
        }
        for (int i = 0; i < this.maxTiles; ++i) {
            this.freeTiles[i] = this.maxTiles - 1 - i;
        }
    }

    private static final class Entry {
        final int[] tileIndices = new int[6];
        final float[] u0 = new float[6];
        final float[] v0 = new float[6];
        float us;
        float vs;
        float radius;
        int emission;
        int dirtyFaceMask;
        int validFaceMask;
        boolean ready;
        boolean staticDirty;
        boolean dynamicDirty;
        boolean urgentStaticDirty;
        boolean boxEmitter;
        int blockedFaceMask;
        double worldX;
        double worldY;
        double worldZ;
        double blockCenterX;
        double blockCenterY;
        double blockCenterZ;
        int framesAlive = 0;
        int refreshCount = 0;
        int stagedPriority = Integer.MAX_VALUE;
        long lastVisibleFrame;
        long neoGpuContentGeneration;

        private Entry() {
        }
    }
}

