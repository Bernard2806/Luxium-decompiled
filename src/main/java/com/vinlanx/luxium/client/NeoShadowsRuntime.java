/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsRenderPipeline;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.TorchRtxState;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsRuntime {
    private final NeoShadowsEngine engine;

    NeoShadowsRuntime(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    void onWorldDirty(@Nullable LongOpenHashSet changedBlocks) {
        if (!this.engine.isSelected()) {
            return;
        }
        this.engine.worldDirty = true;
        this.engine.geometryDirty = true;
        this.engine.skyCaptureDirty = true;
        this.engine.dynamicDirty = true;
        this.engine.framePrepared = false;
        this.engine.pendingRebuild = false;
        this.engine.pendingRebuildSinceMs = 0L;
        this.engine.pendingRebuildFirstRequestMs = 0L;
        ++this.engine.buildEpoch;
        this.engine.visibleCandidateSnapshot = null;
        this.engine.occlusionPipeline.clearOcclusionCache();
        this.requestStaticHotPatchIfUseful(Minecraft.m_91087_().f_91073_, changedBlocks);
    }

    void onLightingDirty(@Nullable LongOpenHashSet changedBlocks, boolean geometryChanged) {
        if (!this.engine.isSelected()) {
            return;
        }
        this.requestStaticHotPatchIfUseful(Minecraft.m_91087_().f_91073_, changedBlocks);
        if (geometryChanged) {
            this.engine.dynamicDirty = true;
            long nowMs = System.currentTimeMillis();
            if (!this.engine.pendingRebuild) {
                this.engine.pendingRebuildFirstRequestMs = nowMs;
                this.engine.pendingRebuildSinceMs = this.engine.highPriorityTracePending ? 0L : nowMs;
                this.engine.highPriorityTracePending = false;
            }
            this.engine.pendingRebuild = true;
        }
        this.engine.skyCaptureDirty = true;
        this.engine.framePrepared = false;
    }

    void consumePendingRebuildIfReady() {
        boolean maxDeferralElapsed;
        if (!this.engine.pendingRebuild) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        boolean quietWindowElapsed = nowMs - this.engine.pendingRebuildSinceMs >= 2L;
        boolean bl = maxDeferralElapsed = nowMs - this.engine.pendingRebuildFirstRequestMs >= 1500L;
        if (!quietWindowElapsed && !maxDeferralElapsed) {
            return;
        }
        this.engine.pendingRebuild = false;
        this.engine.pendingRebuildSinceMs = 0L;
        this.engine.pendingRebuildFirstRequestMs = 0L;
        ++this.engine.buildEpoch;
        if (this.engine.geometryDirty) {
            this.engine.occlusionPipeline.clearOcclusionCache();
            this.engine.visibleCandidateSnapshot = null;
            this.engine.geometryDirty = false;
        }
        this.engine.worldDirty = true;
        this.engine.dynamicDirty = true;
    }

    void prepareFrameInternal(Minecraft mc, Camera camera, float partialTick) {
        this.engine.setEnabled(this.engine.isSelected());
        if (!this.engine.enabled || mc.f_91073_ == null || mc.f_91074_ == null) {
            if ((NeoCpuShadowsEngine.isActive() || GpuNeoShadows.isActive()) && mc.f_91073_ != null && mc.f_91074_ != null) {
                this.engine.framePrepared = true;
                return;
            }
            this.engine.clearRuntimeState();
            this.engine.framePrepared = true;
            return;
        }
        this.consumePendingRebuildIfReady();
        BlockPos center = BlockPos.m_274446_((Position)camera.m_90583_());
        this.applyCompletedStaticResult(center);
        this.applyCompletedStaticHotPatchResult();
        this.applyCompletedDynamicResult(center);
        if (this.engine.activeStaticOutput == null || this.engine.worldDirty || !NeoShadowsRenderPipeline.isCompatibleBuildCenter(this.engine.activeStaticOutput.center, center)) {
            this.scheduleStaticBuild(mc.f_91073_, center);
            this.engine.worldDirty = false;
        }
        this.scheduleDynamicBuild(mc.f_91073_, center, partialTick);
        this.engine.framePrepared = true;
    }

    void requestStaticHotPatchIfUseful(@Nullable ClientLevel level, @Nullable LongOpenHashSet changedBlocks) {
        int e;
        if (level == null || changedBlocks == null || changedBlocks.isEmpty() || this.engine.activeStaticOutput == null || this.engine.activeStaticResultId == 0L) {
            return;
        }
        LongOpenHashSet patchBlocks = new LongOpenHashSet((LongCollection)changedBlocks);
        NeoShadowsTypes.StaticHotPatchRequest existing = this.engine.latestStaticHotPatchRequest;
        if (existing != null && existing.epoch == this.engine.buildEpoch && existing.baseStaticResultId == this.engine.activeStaticResultId && existing.level == level) {
            patchBlocks.addAll((LongCollection)existing.changedBlocks);
        }
        TorchRtxState torchState = TorchRtxState.get();
        int maxEmission = 0;
        LongIterator patchIter = patchBlocks.iterator();
        while (patchIter.hasNext() && ((e = torchState.getSourceEmission(patchIter.nextLong())) <= maxEmission || (maxEmission = e) < 15)) {
        }
        int localRadius = maxEmission > 0 ? Math.min((int)Math.ceil(30.0), (int)Math.ceil(LightRtMath.getMaxDistance(maxEmission)) + 1) : (int)Math.ceil(30.0);
        this.engine.latestStaticHotPatchRequest = new NeoShadowsTypes.StaticHotPatchRequest(this.engine.nextStaticHotPatchRequestId++, this.engine.buildEpoch, this.engine.activeStaticResultId, level, patchBlocks, localRadius);
        if (this.engine.staticHotPatchTaskQueued.compareAndSet(false, true)) {
            this.engine.hotPatchExecutor.submit(this::runStaticHotPatchTask);
        }
    }

    void scheduleStaticBuild(ClientLevel level, BlockPos center) {
        boolean sameRequest;
        NeoShadowsTypes.StaticRequest existing = this.engine.latestStaticRequest;
        boolean bl = sameRequest = existing != null && existing.epoch == this.engine.buildEpoch && existing.level == level && existing.center.m_123341_() == center.m_123341_() && existing.center.m_123342_() == center.m_123342_() && existing.center.m_123343_() == center.m_123343_();
        if (!sameRequest) {
            this.engine.latestStaticRequest = new NeoShadowsTypes.StaticRequest(this.engine.nextStaticRequestId++, this.engine.buildEpoch, level, center.m_7949_());
        }
        if (this.engine.staticTaskQueued.compareAndSet(false, true)) {
            this.engine.staticExecutor.submit(this::runStaticTask);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void runStaticTask() {
        NeoShadowsTypes.StaticRequest request = this.engine.latestStaticRequest;
        try {
            long epoch;
            if (request == null || !this.engine.enabled) {
                return;
            }
            if (request.epoch != this.engine.buildEpoch) {
                return;
            }
            this.engine.inFlightStaticEpoch = epoch = request.epoch;
            this.engine.inFlightStaticPolygons = Collections.emptyList();
            Consumer<List<NeoShadowsTypes.ShadowPolygon>> publisher = polygons -> {
                if (this.engine.buildEpoch != epoch) {
                    return;
                }
                int size = polygons.size();
                if (size == 0 || size == this.engine.inFlightStaticPolygons.size()) {
                    return;
                }
                this.engine.inFlightStaticPolygons = List.copyOf(polygons);
                this.engine.maskReady = false;
                this.engine.skyCaptureDirty = true;
            };
            NeoShadowsTypes.RealtimeStaticShadowOutput output = this.engine.staticPipeline.buildRealtimeStaticShadowOutput(request.level, request.center, publisher);
            if (request.epoch == this.engine.buildEpoch) {
                this.engine.completedStaticResult = new NeoShadowsTypes.StaticResult(request.id, request.epoch, output);
            }
        }
        finally {
            this.engine.staticTaskQueued.set(false);
            NeoShadowsTypes.StaticRequest newest = this.engine.latestStaticRequest;
            if (newest != null && request != null && newest.epoch == this.engine.buildEpoch && newest.id > request.id && this.engine.staticTaskQueued.compareAndSet(false, true)) {
                this.engine.staticExecutor.submit(this::runStaticTask);
            }
        }
    }

    void runStaticHotPatchTask() {
        NeoShadowsTypes.StaticHotPatchRequest request = this.engine.latestStaticHotPatchRequest;
        try {
            if (request == null || !this.engine.enabled || request.baseStaticResultId == 0L) {
                return;
            }
            if (request.epoch != this.engine.buildEpoch) {
                return;
            }
            NeoShadowsTypes.StaticHotPatchOutput output = this.engine.staticPipeline.buildRealtimeChangedBlockShadowOutput(request.level, request.changedBlocks, request.changedBlocks.size(), request.localRadius);
            if (request.epoch == this.engine.buildEpoch) {
                this.engine.completedStaticHotPatchResult = new NeoShadowsTypes.StaticHotPatchResult(request.id, request.epoch, request.baseStaticResultId, output);
            }
        }
        finally {
            this.engine.staticHotPatchTaskQueued.set(false);
            NeoShadowsTypes.StaticHotPatchRequest newest = this.engine.latestStaticHotPatchRequest;
            if (newest != null && request != null && newest.epoch == this.engine.buildEpoch && newest.id > request.id && this.engine.staticHotPatchTaskQueued.compareAndSet(false, true)) {
                this.engine.hotPatchExecutor.submit(this::runStaticHotPatchTask);
            }
        }
    }

    void applyCompletedStaticResult(BlockPos currentCenter) {
        NeoShadowsTypes.StaticResult result = this.engine.completedStaticResult;
        if (result == null) {
            return;
        }
        if (result.epoch != this.engine.buildEpoch || result.id <= this.engine.appliedStaticResultId) {
            this.engine.completedStaticResult = null;
            return;
        }
        if (!NeoShadowsRenderPipeline.isCompatibleBuildCenter(result.output.center, currentCenter)) {
            this.engine.completedStaticResult = null;
            return;
        }
        this.engine.activeStaticOutput = result.output;
        this.engine.appliedStaticResultId = result.id;
        this.engine.activeStaticResultId = result.id;
        this.engine.activeStaticHotPatch = NeoShadowsTypes.StaticHotPatchOverlay.EMPTY;
        this.engine.appliedStaticHotPatchResultId = 0L;
        this.engine.dynamicDirty = true;
        this.engine.skyCaptureDirty = true;
        this.engine.maskReady = false;
        this.engine.inFlightStaticPolygons = Collections.emptyList();
        this.engine.latestStaticHotPatchRequest = null;
        this.engine.completedStaticHotPatchResult = null;
        this.engine.completedStaticResult = null;
    }

    void applyCompletedStaticHotPatchResult() {
        NeoShadowsTypes.StaticHotPatchResult result = this.engine.completedStaticHotPatchResult;
        if (result == null) {
            return;
        }
        if (result.epoch != this.engine.buildEpoch || this.engine.activeStaticOutput == null || result.baseStaticResultId != this.engine.activeStaticResultId || result.id <= this.engine.appliedStaticHotPatchResultId) {
            this.engine.completedStaticHotPatchResult = null;
            return;
        }
        this.engine.activeStaticHotPatch = this.engine.activeStaticHotPatch.merged(result.output);
        this.engine.appliedStaticHotPatchResultId = result.id;
        this.engine.skyCaptureDirty = true;
        this.engine.maskReady = false;
        this.engine.completedStaticHotPatchResult = null;
    }

    void scheduleDynamicBuild(ClientLevel level, BlockPos center, float partialTick) {
        boolean shouldRecaptureMesh;
        boolean levelChanged;
        long previousFingerprint;
        boolean meshRefreshElapsed;
        if (this.engine.activeStaticOutput == null) {
            this.engine.activeStaticHotPatch = NeoShadowsTypes.StaticHotPatchOverlay.EMPTY;
            this.engine.activeDynamicPolygons = Collections.emptyList();
            this.engine.activeDynamicResultId = 0L;
            this.engine.dynamicDirty = true;
            return;
        }
        if (!GpuNeoShadows.entityShadowCastersEnabled()) {
            if (!this.engine.activeDynamicPolygons.isEmpty()) {
                this.engine.activeDynamicPolygons = Collections.emptyList();
                this.engine.activeDynamicResultId = 0L;
                this.engine.maskReady = false;
                this.engine.skyCaptureDirty = true;
            }
            this.engine.latestDynamicCaptureRequest = null;
            this.engine.dynamicCaptureQueued.set(false);
            this.engine.dynamicCasterSnapshot = DynamicShadowMeshCapture.Snapshot.empty();
            this.engine.dynamicSnapshotFingerprint = 0L;
            this.engine.dynamicDirty = false;
            return;
        }
        long nowMs = System.currentTimeMillis();
        long dynamicUpdateIntervalMs = this.getDynamicUpdateIntervalMs();
        boolean staticChanged = this.engine.activeDynamicStaticResultId != this.engine.activeStaticResultId;
        boolean scanElapsed = nowMs - this.engine.lastDynamicStateScanMs >= dynamicUpdateIntervalMs;
        boolean bl = meshRefreshElapsed = nowMs - this.engine.lastDynamicMeshCaptureMs >= dynamicUpdateIntervalMs;
        if (!(this.engine.dynamicDirty || staticChanged || scanElapsed || meshRefreshElapsed)) {
            return;
        }
        DynamicShadowMeshCapture.Snapshot previousSnapshot = this.engine.dynamicCasterSnapshot;
        long nextFingerprint = previousFingerprint = this.engine.dynamicCasterFingerprint;
        boolean sceneChanged = false;
        boolean bl2 = levelChanged = previousSnapshot.level != level;
        if (this.engine.dynamicDirty || scanElapsed || levelChanged) {
            nextFingerprint = this.engine.dynamicPipeline.scanDynamicSceneFingerprint(level, center);
            this.engine.lastDynamicStateScanMs = nowMs;
            sceneChanged = levelChanged || previousFingerprint != nextFingerprint;
        }
        boolean bl3 = shouldRecaptureMesh = levelChanged || sceneChanged || meshRefreshElapsed;
        if (shouldRecaptureMesh) {
            this.requestDynamicCapture(level, center, partialTick, nextFingerprint);
        }
        this.engine.dynamicCasterFingerprint = nextFingerprint;
        if (!this.engine.dynamicDirty && !staticChanged) {
            return;
        }
        this.engine.latestDynamicRequest = new NeoShadowsTypes.DynamicRequest(this.engine.nextDynamicRequestId++, this.engine.buildEpoch, this.engine.activeStaticResultId, level, center.m_7949_(), this.engine.activeStaticOutput, this.engine.dynamicCasterSnapshot);
        this.engine.lastDynamicScheduleMs = nowMs;
        if (this.engine.dynamicTaskQueued.compareAndSet(false, true)) {
            this.engine.dynamicExecutor.submit(this::runDynamicTask);
        }
    }

    void requestDynamicCapture(ClientLevel level, BlockPos center, float partialTick, long fingerprint) {
        this.engine.latestDynamicCaptureRequest = new NeoShadowsTypes.DynamicCaptureRequest(this.engine.nextDynamicCaptureRequestId++, this.engine.buildEpoch, level, center.m_7949_(), partialTick, fingerprint);
        this.engine.dynamicCaptureQueued.set(true);
    }

    void drainDynamicCaptureQueue() {
        if (!this.engine.dynamicCaptureQueued.get()) {
            return;
        }
        NeoShadowsTypes.DynamicCaptureRequest request = this.engine.latestDynamicCaptureRequest;
        if (request == null || !this.engine.enabled || !GpuNeoShadows.entityShadowCastersEnabled()) {
            this.engine.dynamicCaptureQueued.set(false);
            return;
        }
        DynamicShadowMeshCapture.Snapshot snapshot = this.engine.dynamicPipeline.captureDynamicCasterSnapshot(request.level, request.center, request.partialTick);
        if (request.epoch == this.engine.buildEpoch) {
            long snapshotFingerprint = this.engine.dynamicPipeline.fingerprintDynamicSnapshot(snapshot);
            boolean snapshotChanged = snapshot.level != this.engine.dynamicCasterSnapshot.level || snapshotFingerprint != this.engine.dynamicSnapshotFingerprint;
            this.engine.dynamicCasterSnapshot = snapshot;
            this.engine.dynamicCasterFingerprint = request.fingerprint;
            this.engine.dynamicSnapshotFingerprint = snapshotFingerprint;
            if (snapshotChanged) {
                this.engine.dynamicDirty = true;
                this.engine.skyCaptureDirty = true;
                this.engine.maskReady = false;
            }
            this.engine.lastDynamicMeshCaptureMs = System.currentTimeMillis();
        }
        this.engine.dynamicCaptureQueued.set(false);
        NeoShadowsTypes.DynamicCaptureRequest newest = this.engine.latestDynamicCaptureRequest;
        if (newest != null && newest.id > request.id) {
            this.engine.dynamicCaptureQueued.set(true);
        }
    }

    long getDynamicUpdateIntervalMs() {
        int fps = (Integer)Config.CLIENT.entityShadowUpdateFpsLimit.get();
        if (fps < 1) {
            fps = 1;
        }
        return Math.max(1L, 1000L / (long)fps);
    }

    void runDynamicTask() {
        NeoShadowsTypes.DynamicRequest request = this.engine.latestDynamicRequest;
        try {
            if (request == null || !this.engine.enabled) {
                return;
            }
            List<NeoShadowsTypes.ShadowPolygon> polygons = this.engine.dynamicPipeline.buildRealtimeDynamicShadowPolygons(request.level, request.center, request.staticOutput, request.dynamicSnapshot);
            if (request.epoch == this.engine.buildEpoch) {
                this.engine.completedDynamicResult = new NeoShadowsTypes.DynamicResult(request.id, request.epoch, request.staticResultId, request.center, polygons);
            }
        }
        finally {
            this.engine.dynamicTaskQueued.set(false);
            NeoShadowsTypes.DynamicRequest newest = this.engine.latestDynamicRequest;
            if (newest != null && request != null && newest.epoch == this.engine.buildEpoch && newest.id > request.id && this.engine.dynamicTaskQueued.compareAndSet(false, true)) {
                this.engine.dynamicExecutor.submit(this::runDynamicTask);
            }
        }
    }

    void applyCompletedDynamicResult(BlockPos currentCenter) {
        NeoShadowsTypes.DynamicResult result = this.engine.completedDynamicResult;
        if (result == null) {
            return;
        }
        if (result.epoch != this.engine.buildEpoch || result.staticResultId != this.engine.activeStaticResultId) {
            this.engine.completedDynamicResult = null;
            return;
        }
        if (!NeoShadowsRenderPipeline.isCompatibleBuildCenter(result.center, currentCenter)) {
            this.engine.completedDynamicResult = null;
            return;
        }
        if (result.id <= this.engine.activeDynamicResultId) {
            this.engine.completedDynamicResult = null;
            return;
        }
        this.engine.activeDynamicPolygons = result.polygons;
        this.engine.activeDynamicResultId = result.id;
        this.engine.activeDynamicStaticResultId = result.staticResultId;
        this.engine.dynamicDirty = false;
        this.engine.skyCaptureDirty = true;
        this.engine.maskReady = false;
        this.engine.completedDynamicResult = null;
    }
}

