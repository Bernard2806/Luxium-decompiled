/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ReferenceMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSection
 *  me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList
 *  me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable
 *  me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCascade;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class NeoSkyShadowRenderLists
implements ChunkRenderListIterable {
    private static final int GEOMETRY_MASK = 1;
    private static final float SECTION_HALF = 8.0f;
    private final List<ChunkRenderList> lists;

    private NeoSkyShadowRenderLists(List<ChunkRenderList> lists) {
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

    public static BuildResult build(Long2ReferenceMap<RenderSection> sections, NeoSkyCascade nearCascade, boolean buildNear, NeoSkyCascade farCascade, boolean buildFar, int frameId, Cache nearCache, Cache farCache) {
        if (!buildNear && !buildFar) {
            return new BuildResult(null, Float.NaN, false, null, Float.NaN, false);
        }
        BuildAccumulator near = buildNear ? new BuildAccumulator(nearCache, frameId) : null;
        BuildAccumulator far = buildFar ? new BuildAccumulator(farCache, frameId) : null;
        CasterVolume nearVolume = buildNear ? new CasterVolume(nearCascade) : null;
        CasterVolume farVolume = buildFar ? new CasterVolume(farCascade) : null;
        Vector3f scratch = new Vector3f();
        for (RenderSection section : sections.values()) {
            NeoSkyShadowRenderLists.classifySection(section, near, nearVolume, far, farVolume, scratch);
        }
        return new BuildResult(near != null ? near.finish() : null, near != null ? near.maxCasterAxial : Float.NaN, near != null && near.incomplete, far != null ? far.finish() : null, far != null ? far.maxCasterAxial : Float.NaN, far != null && far.incomplete);
    }

    private static void classifySection(RenderSection section, BuildAccumulator near, CasterVolume nearVolume, BuildAccumulator far, CasterVolume farVolume, Vector3f scratch) {
        float farFront;
        if (section.isDisposed()) {
            return;
        }
        float nearFront = near != null ? nearVolume.intersectingCasterFront(section, scratch) : Float.NaN;
        float f = farFront = far != null ? farVolume.intersectingCasterFront(section, scratch) : Float.NaN;
        if (!section.isBuilt()) {
            if (Float.isFinite(nearFront)) {
                near.observeIncomplete(nearFront);
            }
            if (Float.isFinite(farFront)) {
                far.observeIncomplete(farFront);
            }
            return;
        }
        if ((section.getFlags() & 1) == 0) {
            return;
        }
        RenderRegion region = section.getRegion();
        if (region == null) {
            return;
        }
        if (near != null && Float.isFinite(nearFront)) {
            near.add(region, section, nearFront);
        }
        if (far != null && Float.isFinite(farFront)) {
            far.add(region, section, farFront);
        }
    }

    public record BuildResult(NeoSkyShadowRenderLists near, float nearCasterFront, boolean nearIncomplete, NeoSkyShadowRenderLists far, float farCasterFront, boolean farIncomplete) {
    }

    private static final class BuildAccumulator {
        private final Cache cache;
        private final int frameId;
        private final Reference2ObjectLinkedOpenHashMap<RenderRegion, ChunkRenderList> active = new Reference2ObjectLinkedOpenHashMap();
        private float maxCasterAxial = Float.NEGATIVE_INFINITY;
        private boolean incomplete;

        private BuildAccumulator(Cache cache, int frameId) {
            this.cache = cache;
            this.frameId = frameId;
        }

        private void observeIncomplete(float casterFront) {
            this.maxCasterAxial = Math.max(this.maxCasterAxial, casterFront);
            this.incomplete = true;
        }

        private void add(RenderRegion region, RenderSection section, float casterFront) {
            ChunkRenderList list = (ChunkRenderList)this.active.get((Object)region);
            if (list == null) {
                list = this.cache.getOrCreate(region);
                list.reset(this.frameId);
                this.active.put((Object)region, (Object)list);
            }
            list.add(section);
            this.maxCasterAxial = Math.max(this.maxCasterAxial, casterFront);
        }

        private NeoSkyShadowRenderLists finish() {
            this.cache.trimToActive(this.active);
            return new NeoSkyShadowRenderLists(new ArrayList<ChunkRenderList>((Collection<ChunkRenderList>)this.active.values()));
        }
    }

    public static final class Cache {
        private final Reference2ObjectOpenHashMap<RenderRegion, ChunkRenderList> lists = new Reference2ObjectOpenHashMap();

        private ChunkRenderList getOrCreate(RenderRegion region) {
            ChunkRenderList list = (ChunkRenderList)this.lists.get((Object)region);
            if (list == null) {
                list = new ChunkRenderList(region);
                this.lists.put((Object)region, (Object)list);
            }
            return list;
        }

        private void trimToActive(Reference2ObjectLinkedOpenHashMap<RenderRegion, ChunkRenderList> active) {
            int maximumRetained = active.size() * 3 + 64;
            if (this.lists.size() <= maximumRetained) {
                return;
            }
            ObjectIterator iterator = this.lists.keySet().iterator();
            while (iterator.hasNext()) {
                if (active.containsKey(iterator.next())) continue;
                iterator.remove();
            }
        }

        public void clear() {
            this.lists.clear();
        }
    }

    private static final class CasterVolume {
        private final Vec3 center;
        private final Vec3 toLight;
        private final Matrix4f rotation;
        private final float lateralRadius;
        private final float receiverBackAxial;
        private final float sectionHalfX;
        private final float sectionHalfY;
        private final float sectionHalfAxial;

        private CasterVolume(NeoSkyCascade cascade) {
            this.center = cascade.center();
            this.toLight = cascade.toLight();
            this.rotation = cascade.lightViewRotation();
            this.lateralRadius = cascade.projectionRadius();
            this.receiverBackAxial = -cascade.radius() - cascade.receiverGuardBand();
            Vector3f worldX = new Vector3f(8.0f, 0.0f, 0.0f);
            Vector3f worldY = new Vector3f(0.0f, 8.0f, 0.0f);
            Vector3f worldZ = new Vector3f(0.0f, 0.0f, 8.0f);
            this.rotation.transformDirection(worldX);
            this.rotation.transformDirection(worldY);
            this.rotation.transformDirection(worldZ);
            this.sectionHalfX = Math.abs(worldX.x) + Math.abs(worldY.x) + Math.abs(worldZ.x);
            this.sectionHalfY = Math.abs(worldX.y) + Math.abs(worldY.y) + Math.abs(worldZ.y);
            this.sectionHalfAxial = 8.0f * ((float)Math.abs(this.toLight.f_82479_) + (float)Math.abs(this.toLight.f_82480_) + (float)Math.abs(this.toLight.f_82481_));
        }

        private float intersectingCasterFront(RenderSection section, Vector3f scratch) {
            double dx = (double)section.getCenterX() - this.center.f_82479_;
            double dy = (double)section.getCenterY() - this.center.f_82480_;
            double dz = (double)section.getCenterZ() - this.center.f_82481_;
            scratch.set((float)dx, (float)dy, (float)dz);
            this.rotation.transformDirection(scratch);
            if (scratch.x + this.sectionHalfX < -this.lateralRadius || scratch.x - this.sectionHalfX > this.lateralRadius || scratch.y + this.sectionHalfY < -this.lateralRadius || scratch.y - this.sectionHalfY > this.lateralRadius) {
                return Float.NaN;
            }
            double axialCenter = dx * this.toLight.f_82479_ + dy * this.toLight.f_82480_ + dz * this.toLight.f_82481_;
            float maxAxial = (float)axialCenter + this.sectionHalfAxial;
            if (maxAxial < this.receiverBackAxial) {
                return Float.NaN;
            }
            return maxAxial;
        }
    }
}

