/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Vector4f
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class DynamicShadowMeshCapture {
    private static final int MAX_QUADS_PER_CASTER = 192;
    private static final int MAX_CAPTURED_VERTICES = 768;

    private DynamicShadowMeshCapture() {
    }

    public static Snapshot capture(ClientLevel level, BlockPos center, double radius, int maxCasters, float partialTick) {
        Minecraft mc = Minecraft.m_91087_();
        EntityRenderDispatcher entityDispatcher = mc.m_91290_();
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.m_167982_();
        entityDispatcher.m_114408_((Level)level, mc.f_91063_.m_109153_(), mc.m_91288_());
        blockEntityDispatcher.m_173564_((Level)level, mc.f_91063_.m_109153_(), mc.f_91077_);
        Vec3 centerPos = Vec3.m_82512_((Vec3i)center);
        double radiusSq = radius * radius;
        Vec3 cameraPos = mc.f_91063_.m_109153_().m_90583_();
        ArrayList<CapturedCaster> casters = new ArrayList<CapturedCaster>(Math.min(maxCasters, 64));
        long nextId = 1L;
        for (Entity entity : level.m_104735_()) {
            CapturedCaster captured;
            AABB box;
            if (casters.size() >= maxCasters) {
                return new Snapshot(level, List.copyOf(casters));
            }
            if (entity == null || entity.m_213877_() || entity.m_20145_() || (box = entity.m_20191_()) == null || DynamicShadowMeshCapture.distanceToAabbSq(centerPos, box) > radiusSq || (captured = DynamicShadowMeshCapture.captureEntity(entityDispatcher, entity, nextId++, partialTick, box.m_82400_(0.03))) == null) continue;
            casters.add(captured);
        }
        int chunkRadius = Math.max(1, (int)Math.ceil(radius / 16.0));
        int centerChunkX = center.m_123341_() >> 4;
        int centerChunkZ = center.m_123343_() >> 4;
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                if (casters.size() >= maxCasters) {
                    return new Snapshot(level, List.copyOf(casters));
                }
                LevelChunk chunk = level.m_7726_().m_7131_(centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                    CapturedCaster captured;
                    BlockEntityRenderer renderer;
                    if (casters.size() >= maxCasters) {
                        return new Snapshot(level, List.copyOf(casters));
                    }
                    if (blockEntity == null || blockEntity.m_58901_() || (renderer = blockEntityDispatcher.m_112265_(blockEntity)) == null || !renderer.m_142756_(blockEntity, cameraPos)) continue;
                    AABB box = blockEntity.getRenderBoundingBox();
                    if (box == null) {
                        box = new AABB(blockEntity.m_58899_());
                    }
                    if (DynamicShadowMeshCapture.distanceToAabbSq(centerPos, box) > radiusSq || (captured = DynamicShadowMeshCapture.captureBlockEntity(renderer, blockEntity, nextId++, partialTick, box.m_82400_(0.02))) == null) continue;
                    casters.add(captured);
                }
            }
        }
        return new Snapshot(level, List.copyOf(casters));
    }

    @Nullable
    private static <E extends Entity> CapturedCaster captureEntity(EntityRenderDispatcher dispatcher, E entity, long id, float partialTick, AABB fallbackBounds) {
        EntityRenderer renderer = dispatcher.m_114382_(entity);
        if (renderer == null) {
            return null;
        }
        QuadCaptureBufferSource capture = new QuadCaptureBufferSource();
        PoseStack poseStack = new PoseStack();
        Vec3 renderOffset = renderer.m_7860_(entity, partialTick);
        double renderX = Mth.m_14139_((double)partialTick, (double)entity.f_19790_, (double)entity.m_20185_()) + renderOffset.f_82479_;
        double renderY = Mth.m_14139_((double)partialTick, (double)entity.f_19791_, (double)entity.m_20186_()) + renderOffset.f_82480_;
        double renderZ = Mth.m_14139_((double)partialTick, (double)entity.f_19792_, (double)entity.m_20189_()) + renderOffset.f_82481_;
        poseStack.m_85837_(renderX, renderY, renderZ);
        int packedLight = dispatcher.m_114394_(entity, partialTick);
        float renderYaw = Mth.m_14179_((float)partialTick, (float)entity.f_19859_, (float)entity.m_146908_());
        renderer.m_7392_(entity, renderYaw, partialTick, poseStack, (MultiBufferSource)capture, packedLight);
        return capture.buildCaster(id, fallbackBounds);
    }

    @Nullable
    private static CapturedCaster captureBlockEntity(BlockEntityRenderer renderer, BlockEntity blockEntity, long id, float partialTick, AABB fallbackBounds) {
        QuadCaptureBufferSource capture = new QuadCaptureBufferSource();
        PoseStack poseStack = new PoseStack();
        BlockPos pos = blockEntity.m_58899_();
        poseStack.m_252880_((float)pos.m_123341_(), (float)pos.m_123342_(), (float)pos.m_123343_());
        int packedLight = blockEntity.m_58904_() != null ? LevelRenderer.m_109541_((BlockAndTintGetter)blockEntity.m_58904_(), (BlockPos)pos) : 0xF000F0;
        renderer.m_6922_(blockEntity, partialTick, poseStack, (MultiBufferSource)capture, packedLight, OverlayTexture.f_118083_);
        return capture.buildCaster(id, fallbackBounds);
    }

    private static double distanceToAabbSq(Vec3 point, AABB box) {
        double dx = 0.0;
        if (point.f_82479_ < box.f_82288_) {
            dx = box.f_82288_ - point.f_82479_;
        } else if (point.f_82479_ > box.f_82291_) {
            dx = point.f_82479_ - box.f_82291_;
        }
        double dy = 0.0;
        if (point.f_82480_ < box.f_82289_) {
            dy = box.f_82289_ - point.f_82480_;
        } else if (point.f_82480_ > box.f_82292_) {
            dy = point.f_82480_ - box.f_82292_;
        }
        double dz = 0.0;
        if (point.f_82481_ < box.f_82290_) {
            dz = box.f_82290_ - point.f_82481_;
        } else if (point.f_82481_ > box.f_82293_) {
            dz = point.f_82481_ - box.f_82293_;
        }
        return dx * dx + dy * dy + dz * dz;
    }

    public static final class Snapshot {
        private static final Snapshot EMPTY = new Snapshot(null, Collections.emptyList());
        @Nullable
        public final ClientLevel level;
        public final List<CapturedCaster> casters;

        Snapshot(@Nullable ClientLevel level, List<CapturedCaster> casters) {
            this.level = level;
            this.casters = casters;
        }

        public static Snapshot empty() {
            return EMPTY;
        }
    }

    public static final class CapturedCaster {
        public final long id;
        public final AABB bounds;
        public final List<Vec3[]> quads;

        CapturedCaster(long id, AABB bounds, List<Vec3[]> quads) {
            this.id = id;
            this.bounds = bounds;
            this.quads = quads;
        }
    }

    private static final class QuadCaptureBufferSource
    implements MultiBufferSource {
        private final Map<RenderType, CapturingVertexConsumer> consumers = new HashMap<RenderType, CapturingVertexConsumer>();

        private QuadCaptureBufferSource() {
        }

        public VertexConsumer m_6299_(RenderType renderType) {
            return this.consumers.computeIfAbsent(renderType, CapturingVertexConsumer::new);
        }

        @Nullable
        CapturedCaster buildCaster(long id, AABB fallbackBounds) {
            ArrayList<Vec3[]> quads = new ArrayList<Vec3[]>();
            for (CapturingVertexConsumer consumer : this.consumers.values()) {
                consumer.appendInto(quads);
                if (quads.size() < 192) continue;
                break;
            }
            if (quads.isEmpty()) {
                return new CapturedCaster(id, fallbackBounds, Collections.emptyList());
            }
            return new CapturedCaster(id, QuadCaptureBufferSource.quadBounds(quads).m_82400_(0.02), List.copyOf(quads));
        }

        private static AABB quadBounds(List<Vec3[]> quads) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (Vec3[] quad : quads) {
                for (Vec3 vertex : quad) {
                    minX = Math.min(minX, vertex.f_82479_);
                    minY = Math.min(minY, vertex.f_82480_);
                    minZ = Math.min(minZ, vertex.f_82481_);
                    maxX = Math.max(maxX, vertex.f_82479_);
                    maxY = Math.max(maxY, vertex.f_82480_);
                    maxZ = Math.max(maxZ, vertex.f_82481_);
                }
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private record VertexRecord(double x, double y, double z, int alpha) {
        Vec3 position() {
            return new Vec3(this.x, this.y, this.z);
        }
    }

    private static final class CapturingVertexConsumer
    implements VertexConsumer {
        private static final int MIN_VERTEX_ALPHA = 16;
        private final boolean capture;
        private final VertexFormat.Mode mode;
        private final List<Vec3[]> quads = new ArrayList<Vec3[]>();
        private final VertexRecord[] pending = new VertexRecord[4];
        private int pendingCount;
        private int capturedVertexCount;
        private int defaultR = 255;
        private int defaultG = 255;
        private int defaultB = 255;
        private int defaultA = 255;
        private boolean useDefaultColor;
        private double x;
        private double y;
        private double z;
        private int a = 255;

        CapturingVertexConsumer(RenderType renderType) {
            this.capture = renderType.m_173186_() == VertexFormat.Mode.QUADS;
            this.mode = renderType.m_173186_();
        }

        public VertexConsumer m_5483_(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public VertexConsumer m_6122_(int red, int green, int blue, int alpha) {
            this.a = alpha;
            return this;
        }

        public VertexConsumer m_7421_(float u, float v) {
            return this;
        }

        public VertexConsumer m_7122_(int u, int v) {
            return this;
        }

        public VertexConsumer m_7120_(int u, int v) {
            return this;
        }

        public VertexConsumer m_5601_(float x, float y, float z) {
            return this;
        }

        public void m_5752_() {
            if (!this.capture || this.mode != VertexFormat.Mode.QUADS || this.quads.size() >= 192 || this.capturedVertexCount >= 768) {
                this.pendingCount = this.mode == VertexFormat.Mode.QUADS && this.pendingCount >= 4 ? 0 : this.pendingCount;
                return;
            }
            int alpha = this.useDefaultColor ? this.defaultA : this.a;
            this.pending[this.pendingCount++] = new VertexRecord(this.x, this.y, this.z, alpha);
            ++this.capturedVertexCount;
            if (this.pendingCount == 4) {
                int averageAlpha = (this.pending[0].alpha + this.pending[1].alpha + this.pending[2].alpha + this.pending[3].alpha) / 4;
                if (averageAlpha >= 16) {
                    this.quads.add(new Vec3[]{this.pending[0].position(), this.pending[1].position(), this.pending[2].position(), this.pending[3].position()});
                }
                this.pendingCount = 0;
            }
        }

        public void putBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int overlay, boolean readExistingColor) {
            if (!this.capture || this.mode != VertexFormat.Mode.QUADS || this.quads.size() >= 192 || this.capturedVertexCount + 4 > 768) {
                return;
            }
            int[] vertices = bakedQuad.m_111303_();
            if (vertices.length < 32) {
                return;
            }
            Matrix4f matrix = pose.m_252922_();
            Vec3[] quad = new Vec3[4];
            int alphaByte = Mth.m_14045_((int)((int)(alpha * 255.0f)), (int)0, (int)255);
            for (int index = 0; index < 4; ++index) {
                int vertexBase = index * 8;
                float x = Float.intBitsToFloat(vertices[vertexBase]);
                float y = Float.intBitsToFloat(vertices[vertexBase + 1]);
                float z = Float.intBitsToFloat(vertices[vertexBase + 2]);
                Vector4f transformed = matrix.transform(new Vector4f(x, y, z, 1.0f));
                quad[index] = new Vec3((double)transformed.x(), (double)transformed.y(), (double)transformed.z());
                int bakedAlpha = vertices[vertexBase + 3] >>> 24;
                if (bakedAlpha <= alphaByte) continue;
                alphaByte = bakedAlpha;
            }
            if (alphaByte >= 16) {
                this.quads.add(quad);
                this.capturedVertexCount += 4;
            }
        }

        public void m_7404_(int red, int green, int blue, int alpha) {
            this.defaultR = red;
            this.defaultG = green;
            this.defaultB = blue;
            this.defaultA = alpha;
            this.useDefaultColor = true;
        }

        public void m_141991_() {
            this.useDefaultColor = false;
        }

        void appendInto(List<Vec3[]> output) {
            int limit = Math.min(this.quads.size(), 192 - output.size());
            for (int index = 0; index < limit; ++index) {
                output.add(this.quads.get(index));
            }
        }
    }
}
