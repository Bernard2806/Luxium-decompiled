/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShape;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

final class NeoCpuShadowShapeBaker {
    private static final double SURFACE_EPSILON = 1.0E-5;
    private static final double SHADOW_EPSILON = 1.0E-6;
    private static final Direction[] ALL_FACES = Direction.values();
    private static final int SPRITE_ALPHA_THRESHOLD = 16;
    private static final float MIN_MODEL_OPACITY = 0.003921569f;
    private static final NeoCpuShadowTypes.Box[] EMPTY_BOXES = new NeoCpuShadowTypes.Box[0];
    private static final NeoCpuShadowShape.LocalQuadTemplate[] EMPTY_QUADS = new NeoCpuShadowShape.LocalQuadTemplate[0];
    private static final ThreadLocal<RandomSource> RANDOM_SOURCE_TL = ThreadLocal.withInitial(RandomSource::m_216327_);
    private final ConcurrentHashMap<TextureAtlasSprite, SpriteAlphaMask> spriteMaskCache = new ConcurrentHashMap();

    NeoCpuShadowShapeBaker() {
    }

    NeoCpuShadowShape bake(BlockState state, ClientLevel level, BlockPos pos) {
        NeoCpuShadowTypes.Box[] occluderBoxes;
        List<AABB> surfaceBoxes = NeoCpuShadowShapeBaker.collectRenderSurfaceBoxes(state, level, pos);
        NeoCpuShadowShape.LocalQuadTemplate[] modelOccluders = this.collectModelOccluders(state, level, pos);
        NeoCpuShadowTypes.Box[] boxArray = occluderBoxes = modelOccluders.length > 0 ? EMPTY_BOXES : NeoCpuShadowShapeBaker.collectOccluderBoxes(state, level, pos);
        if (surfaceBoxes.isEmpty() && occluderBoxes.length == 0 && modelOccluders.length == 0) {
            return NeoCpuShadowShape.EMPTY;
        }
        ArrayList<NeoCpuShadowShape.ReceiverTemplate> receivers = new ArrayList<NeoCpuShadowShape.ReceiverTemplate>(surfaceBoxes.size() * 6);
        for (int boxIndex = 0; boxIndex < surfaceBoxes.size(); ++boxIndex) {
            AABB box = surfaceBoxes.get(boxIndex);
            for (Direction face : ALL_FACES) {
                SurfaceRect initialRect = NeoCpuShadowShapeBaker.canonicalFaceRect(face, box);
                if (initialRect == null) continue;
                double plane = NeoCpuShadowShapeBaker.facePlaneCoordinate(face, box);
                List<SurfaceRect> fragments = new ArrayList<SurfaceRect>();
                fragments.add(initialRect);
                for (int otherIndex = 0; otherIndex < surfaceBoxes.size() && !fragments.isEmpty(); ++otherIndex) {
                    AABB other;
                    if (otherIndex == boxIndex || !NeoCpuShadowShapeBaker.coversFacePlane(face, plane, other = surfaceBoxes.get(otherIndex))) continue;
                    fragments = NeoCpuShadowShapeBaker.subtractSurfaceRectList(fragments, NeoCpuShadowShapeBaker.canonicalFaceRect(face, other));
                }
                for (SurfaceRect fragment : fragments) {
                    if (fragment == null || fragment.width() <= 1.0E-5 || fragment.height() <= 1.0E-5) continue;
                    receivers.add(new NeoCpuShadowShape.ReceiverTemplate(face, plane, fragment.minU, fragment.maxU, fragment.minV, fragment.maxV));
                }
            }
        }
        boolean floodBlocking = state.m_60815_() && state.m_60838_((BlockGetter)level, pos);
        return new NeoCpuShadowShape(occluderBoxes, modelOccluders, receivers.toArray(new NeoCpuShadowShape.ReceiverTemplate[0]), floodBlocking);
    }

    private NeoCpuShadowShape.LocalQuadTemplate[] collectModelOccluders(BlockState state, ClientLevel level, BlockPos pos) {
        if (state.m_60799_() != RenderShape.MODEL) {
            return EMPTY_QUADS;
        }
        BakedModel model = Minecraft.m_91087_().m_91289_().m_110910_(state);
        if (model == null) {
            return EMPTY_QUADS;
        }
        List<BakedQuad> quads = NeoCpuShadowShapeBaker.collectModelQuads(model, state);
        if (quads.isEmpty()) {
            return EMPTY_QUADS;
        }
        boolean needsModelOccluders = !state.m_60838_((BlockGetter)level, pos);
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.m_173410_();
            if (sprite == null) continue;
            SpriteAlphaMask mask = this.spriteMaskCache.computeIfAbsent(sprite, this::buildSpriteMask);
            if (mask.fullyOpaque && !(mask.averageOpacity < 0.999f)) continue;
            needsModelOccluders = true;
            break;
        }
        if (!needsModelOccluders) {
            return EMPTY_QUADS;
        }
        ArrayList<NeoCpuShadowShape.LocalQuadTemplate> result = new ArrayList<NeoCpuShadowShape.LocalQuadTemplate>(quads.size() * 4);
        for (BakedQuad quad : quads) {
            LocalTexturedQuad texturedQuad;
            TextureAtlasSprite sprite = quad.m_173410_();
            if (sprite == null || (texturedQuad = LocalTexturedQuad.from(quad)) == null) continue;
            SpriteAlphaMask mask = this.spriteMaskCache.computeIfAbsent(sprite, this::buildSpriteMask);
            if (mask.fullyTransparent || mask.averageOpacity < 0.003921569f) continue;
            if (mask.fullyOpaque) {
                result.add(new NeoCpuShadowShape.LocalQuadTemplate(texturedQuad.worldVertices, mask.averageOpacity));
                continue;
            }
            SpritePixelBounds usedPixels = NeoCpuShadowShapeBaker.computeQuadPixelBounds(texturedQuad, sprite);
            if (usedPixels == null) continue;
            for (SpriteRect rect : mask.rectangles) {
                Vec3[] vertices;
                double minPixelX = Math.max((double)rect.minX, usedPixels.minX);
                double maxPixelX = Math.min((double)rect.maxX, usedPixels.maxX);
                double minPixelY = Math.max((double)rect.minY, usedPixels.minY);
                double maxPixelY = Math.min((double)rect.maxY, usedPixels.maxY);
                if (maxPixelX - minPixelX <= 1.0E-6 || maxPixelY - minPixelY <= 1.0E-6 || (vertices = NeoCpuShadowShapeBaker.buildOpaqueRectVertices(texturedQuad, sprite, minPixelX, maxPixelX, minPixelY, maxPixelY)) == null) continue;
                result.add(new NeoCpuShadowShape.LocalQuadTemplate(vertices, mask.averageOpacity));
            }
        }
        return result.isEmpty() ? EMPTY_QUADS : result.toArray(EMPTY_QUADS);
    }

    private static List<AABB> collectRenderSurfaceBoxes(BlockState state, ClientLevel level, BlockPos pos) {
        VoxelShape shape = state.m_60808_((BlockGetter)level, pos);
        if (shape.m_83281_()) {
            return List.of();
        }
        List<AABB> aabbs = shape.m_83299_();
        if (aabbs.isEmpty()) {
            return List.of();
        }
        ArrayList<AABB> filtered = new ArrayList<AABB>(aabbs.size());
        for (AABB aabb : aabbs) {
            if (aabb.m_82362_() <= 1.0E-5 || aabb.m_82376_() <= 1.0E-5 || aabb.m_82385_() <= 1.0E-5) continue;
            filtered.add(aabb);
        }
        return filtered;
    }

    private static NeoCpuShadowTypes.Box[] collectOccluderBoxes(BlockState state, ClientLevel level, BlockPos pos) {
        VoxelShape shape = state.m_60812_((BlockGetter)level, pos);
        if (shape.m_83281_()) {
            return EMPTY_BOXES;
        }
        if (state.m_60838_((BlockGetter)level, pos)) {
            return new NeoCpuShadowTypes.Box[]{new NeoCpuShadowTypes.Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0L, 1.0f)};
        }
        ArrayList<NeoCpuShadowTypes.Box> boxes = new ArrayList<>();
        shape.m_83286_((minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (maxX - minX <= 1.0E-5 || maxY - minY <= 1.0E-5 || maxZ - minZ <= 1.0E-5) {
                return;
            }
            boxes.add(new NeoCpuShadowTypes.Box(minX, minY, minZ, maxX, maxY, maxZ, 0L, 1.0f));
        });
        return boxes.isEmpty() ? EMPTY_BOXES : boxes.toArray(EMPTY_BOXES);
    }

    private static List<BakedQuad> collectModelQuads(BakedModel model, BlockState state) {
        ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
        RandomSource random = RANDOM_SOURCE_TL.get();
        for (Direction face : ALL_FACES) {
            random.m_188584_(42L);
            quads.addAll(model.m_213637_(state, face, random));
        }
        random.m_188584_(42L);
        quads.addAll(model.m_213637_(state, null, random));
        return quads;
    }

    private SpriteAlphaMask buildSpriteMask(TextureAtlasSprite sprite) {
        float averageOpacity;
        int width = Math.max(1, sprite.m_245424_().m_246492_());
        int height = Math.max(1, sprite.m_245424_().m_245330_());
        boolean[] opaque = new boolean[width * height];
        boolean anyOpaque = false;
        boolean anyTransparent = false;
        long opaqueAlphaSum = 0L;
        int opaquePixelCount = 0;
        for (int pixelY = 0; pixelY < height; ++pixelY) {
            int rowOffset = pixelY * width;
            for (int pixelX = 0; pixelX < width; ++pixelX) {
                boolean solid;
                int alpha = FastColor.ARGB32.m_13655_((int)sprite.getPixelRGBA(0, pixelX, pixelY));
                opaque[rowOffset + pixelX] = solid = alpha >= 16;
                anyOpaque |= solid;
                anyTransparent |= !solid;
                if (!solid) continue;
                opaqueAlphaSum += (long)alpha;
                ++opaquePixelCount;
            }
        }
        if (!anyOpaque) {
            return SpriteAlphaMask.transparentMask();
        }
        float f = averageOpacity = opaquePixelCount <= 0 ? 0.0f : Mth.m_14036_((float)((float)opaqueAlphaSum / (float)((double)opaquePixelCount * 255.0)), (float)0.0f, (float)1.0f);
        if (!anyTransparent) {
            return averageOpacity >= 0.999f ? SpriteAlphaMask.opaqueMask() : new SpriteAlphaMask(new SpriteRect[0], true, false, averageOpacity);
        }
        boolean[] consumed = new boolean[opaque.length];
        ArrayList<SpriteRect> rectangles = new ArrayList<SpriteRect>();
        for (int pixelY = 0; pixelY < height; ++pixelY) {
            for (int pixelX = 0; pixelX < width; ++pixelX) {
                int nextIndex;
                int index = pixelY * width + pixelX;
                if (!opaque[index] || consumed[index]) continue;
                int rectWidth = 1;
                while (pixelX + rectWidth < width && opaque[nextIndex = pixelY * width + pixelX + rectWidth] && !consumed[nextIndex]) {
                    ++rectWidth;
                }
                int rectHeight = 1;
                boolean canGrow = true;
                while (pixelY + rectHeight < height && canGrow) {
                    int nextRowOffset = (pixelY + rectHeight) * width;
                    for (int scanX = 0; scanX < rectWidth; ++scanX) {
                        int nextIndex2 = nextRowOffset + pixelX + scanX;
                        if (opaque[nextIndex2] && !consumed[nextIndex2]) continue;
                        canGrow = false;
                        break;
                    }
                    if (!canGrow) continue;
                    ++rectHeight;
                }
                for (int fillY = 0; fillY < rectHeight; ++fillY) {
                    int fillRowOffset = (pixelY + fillY) * width;
                    for (int fillX = 0; fillX < rectWidth; ++fillX) {
                        consumed[fillRowOffset + pixelX + fillX] = true;
                    }
                }
                rectangles.add(new SpriteRect(pixelX, pixelX + rectWidth, pixelY, pixelY + rectHeight));
            }
        }
        return new SpriteAlphaMask(rectangles.toArray(new SpriteRect[0]), false, false, averageOpacity);
    }

    @Nullable
    private static SpritePixelBounds computeQuadPixelBounds(LocalTexturedQuad quad, TextureAtlasSprite sprite) {
        int spriteWidth = Math.max(1, sprite.m_245424_().m_246492_());
        int spriteHeight = Math.max(1, sprite.m_245424_().m_245330_());
        double minPixelX = Double.POSITIVE_INFINITY;
        double maxPixelX = Double.NEGATIVE_INFINITY;
        double minPixelY = Double.POSITIVE_INFINITY;
        double maxPixelY = Double.NEGATIVE_INFINITY;
        for (AtlasUv uv : quad.atlasUvs) {
            double pixelX = (double)(sprite.m_174727_((float)uv.u) * (float)spriteWidth) / 16.0;
            double pixelY = (double)(sprite.m_174741_((float)uv.v) * (float)spriteHeight) / 16.0;
            minPixelX = Math.min(minPixelX, pixelX);
            maxPixelX = Math.max(maxPixelX, pixelX);
            minPixelY = Math.min(minPixelY, pixelY);
            maxPixelY = Math.max(maxPixelY, pixelY);
        }
        if (!(Double.isFinite(minPixelX) && Double.isFinite(minPixelY) && Double.isFinite(maxPixelX) && Double.isFinite(maxPixelY))) {
            return null;
        }
        minPixelX = Mth.m_14008_((double)minPixelX, (double)0.0, (double)spriteWidth);
        maxPixelX = Mth.m_14008_((double)maxPixelX, (double)0.0, (double)spriteWidth);
        minPixelY = Mth.m_14008_((double)minPixelY, (double)0.0, (double)spriteHeight);
        maxPixelY = Mth.m_14008_((double)maxPixelY, (double)0.0, (double)spriteHeight);
        if (maxPixelX - minPixelX <= 1.0E-6 || maxPixelY - minPixelY <= 1.0E-6) {
            return null;
        }
        return new SpritePixelBounds(minPixelX, maxPixelX, minPixelY, maxPixelY);
    }

    @Nullable
    private static Vec3[] buildOpaqueRectVertices(LocalTexturedQuad quad, TextureAtlasSprite sprite, double minPixelX, double maxPixelX, double minPixelY, double maxPixelY) {
        int spriteWidth = Math.max(1, sprite.m_245424_().m_246492_());
        int spriteHeight = Math.max(1, sprite.m_245424_().m_245330_());
        double minAtlasU = sprite.m_118367_(minPixelX * 16.0 / (double)spriteWidth);
        double maxAtlasU = sprite.m_118367_(maxPixelX * 16.0 / (double)spriteWidth);
        double minAtlasV = sprite.m_118393_(minPixelY * 16.0 / (double)spriteHeight);
        double maxAtlasV = sprite.m_118393_(maxPixelY * 16.0 / (double)spriteHeight);
        Vec3 topLeft = NeoCpuShadowShapeBaker.mapAtlasUvToWorld(quad, minAtlasU, minAtlasV);
        Vec3 topRight = NeoCpuShadowShapeBaker.mapAtlasUvToWorld(quad, maxAtlasU, minAtlasV);
        Vec3 bottomRight = NeoCpuShadowShapeBaker.mapAtlasUvToWorld(quad, maxAtlasU, maxAtlasV);
        Vec3 bottomLeft = NeoCpuShadowShapeBaker.mapAtlasUvToWorld(quad, minAtlasU, maxAtlasV);
        if (topLeft == null || topRight == null || bottomRight == null || bottomLeft == null) {
            return null;
        }
        return new Vec3[]{topLeft, topRight, bottomRight, bottomLeft};
    }

    @Nullable
    private static Vec3 mapAtlasUvToWorld(LocalTexturedQuad quad, double atlasU, double atlasV) {
        Vec3 mapped = NeoCpuShadowShapeBaker.mapAtlasUvToWorldTriangle(quad, 0, 1, 2, atlasU, atlasV);
        if (mapped != null) {
            return mapped;
        }
        return NeoCpuShadowShapeBaker.mapAtlasUvToWorldTriangle(quad, 0, 2, 3, atlasU, atlasV);
    }

    @Nullable
    private static Vec3 mapAtlasUvToWorldTriangle(LocalTexturedQuad quad, int indexA, int indexB, int indexC, double atlasU, double atlasV) {
        AtlasUv uvA = quad.atlasUvs[indexA];
        AtlasUv uvB = quad.atlasUvs[indexB];
        AtlasUv uvC = quad.atlasUvs[indexC];
        double denominator = (uvB.v - uvC.v) * (uvA.u - uvC.u) + (uvC.u - uvB.u) * (uvA.v - uvC.v);
        if (Math.abs(denominator) <= 1.0E-6) {
            return null;
        }
        double weightA = ((uvB.v - uvC.v) * (atlasU - uvC.u) + (uvC.u - uvB.u) * (atlasV - uvC.v)) / denominator;
        double weightB = ((uvC.v - uvA.v) * (atlasU - uvC.u) + (uvA.u - uvC.u) * (atlasV - uvC.v)) / denominator;
        double weightC = 1.0 - weightA - weightB;
        double epsilon = 1.0E-4;
        if (weightA < -epsilon || weightB < -epsilon || weightC < -epsilon) {
            return null;
        }
        Vec3 vertexA = quad.worldVertices[indexA];
        Vec3 vertexB = quad.worldVertices[indexB];
        Vec3 vertexC = quad.worldVertices[indexC];
        return new Vec3(vertexA.f_82479_ * weightA + vertexB.f_82479_ * weightB + vertexC.f_82479_ * weightC, vertexA.f_82480_ * weightA + vertexB.f_82480_ * weightB + vertexC.f_82480_ * weightC, vertexA.f_82481_ * weightA + vertexB.f_82481_ * weightB + vertexC.f_82481_ * weightC);
    }

    private static List<SurfaceRect> subtractSurfaceRectList(List<SurfaceRect> sourceRects, SurfaceRect coverRect) {
        if (coverRect == null || sourceRects.isEmpty()) {
            return sourceRects;
        }
        ArrayList<SurfaceRect> result = new ArrayList<SurfaceRect>(sourceRects.size());
        for (SurfaceRect rect : sourceRects) {
            NeoCpuShadowShapeBaker.subtractSurfaceRect(rect, coverRect, result);
        }
        return result;
    }

    private static void subtractSurfaceRect(SurfaceRect sourceRect, SurfaceRect coverRect, List<SurfaceRect> out) {
        double overlapMinU = Math.max(sourceRect.minU, coverRect.minU);
        double overlapMaxU = Math.min(sourceRect.maxU, coverRect.maxU);
        double overlapMinV = Math.max(sourceRect.minV, coverRect.minV);
        double overlapMaxV = Math.min(sourceRect.maxV, coverRect.maxV);
        if (overlapMaxU - overlapMinU <= 1.0E-5 || overlapMaxV - overlapMinV <= 1.0E-5) {
            out.add(sourceRect);
            return;
        }
        if (overlapMinV - sourceRect.minV > 1.0E-5) {
            out.add(new SurfaceRect(sourceRect.minU, sourceRect.maxU, sourceRect.minV, overlapMinV));
        }
        if (sourceRect.maxV - overlapMaxV > 1.0E-5) {
            out.add(new SurfaceRect(sourceRect.minU, sourceRect.maxU, overlapMaxV, sourceRect.maxV));
        }
        if (overlapMinU - sourceRect.minU > 1.0E-5) {
            out.add(new SurfaceRect(sourceRect.minU, overlapMinU, overlapMinV, overlapMaxV));
        }
        if (sourceRect.maxU - overlapMaxU > 1.0E-5) {
            out.add(new SurfaceRect(overlapMaxU, sourceRect.maxU, overlapMinV, overlapMaxV));
        }
    }

    private static boolean coversFacePlane(Direction face, double planeCoordinate, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> {
                if (Math.abs(box.f_82289_ - planeCoordinate) <= 1.0E-5) {
                    yield true;
                }
                yield false;
            }
            case Direction.DOWN -> {
                if (Math.abs(box.f_82292_ - planeCoordinate) <= 1.0E-5) {
                    yield true;
                }
                yield false;
            }
            case Direction.NORTH -> {
                if (Math.abs(box.f_82293_ - planeCoordinate) <= 1.0E-5) {
                    yield true;
                }
                yield false;
            }
            case Direction.SOUTH -> {
                if (Math.abs(box.f_82290_ - planeCoordinate) <= 1.0E-5) {
                    yield true;
                }
                yield false;
            }
            case Direction.WEST -> {
                if (Math.abs(box.f_82291_ - planeCoordinate) <= 1.0E-5) {
                    yield true;
                }
                yield false;
            }
            case Direction.EAST -> Math.abs(box.f_82288_ - planeCoordinate) <= 1.0E-5;
        };
    }

    private static double facePlaneCoordinate(Direction face, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> box.f_82292_;
            case Direction.DOWN -> box.f_82289_;
            case Direction.NORTH -> box.f_82290_;
            case Direction.SOUTH -> box.f_82293_;
            case Direction.WEST -> box.f_82288_;
            case Direction.EAST -> box.f_82291_;
        };
    }

    private static SurfaceRect canonicalFaceRect(Direction face, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> NeoCpuShadowShapeBaker.surfaceRect(box.f_82288_, box.f_82291_, box.f_82290_, box.f_82293_);
            case Direction.NORTH, Direction.SOUTH -> NeoCpuShadowShapeBaker.surfaceRect(box.f_82288_, box.f_82291_, box.f_82289_, box.f_82292_);
            case Direction.WEST, Direction.EAST -> NeoCpuShadowShapeBaker.surfaceRect(box.f_82290_, box.f_82293_, box.f_82289_, box.f_82292_);
        };
    }

    private static SurfaceRect surfaceRect(double minU, double maxU, double minV, double maxV) {
        return maxU - minU <= 1.0E-5 || maxV - minV <= 1.0E-5 ? null : new SurfaceRect(minU, maxU, minV, maxV);
    }

    private record SurfaceRect(double minU, double maxU, double minV, double maxV) {
        double width() {
            return this.maxU - this.minU;
        }

        double height() {
            return this.maxV - this.minV;
        }
    }

    private record SpriteAlphaMask(SpriteRect[] rectangles, boolean fullyOpaque, boolean fullyTransparent, float averageOpacity) {
        private static final SpriteAlphaMask FULLY_OPAQUE = new SpriteAlphaMask(new SpriteRect[0], true, false, 1.0f);
        private static final SpriteAlphaMask FULLY_TRANSPARENT = new SpriteAlphaMask(new SpriteRect[0], false, true, 0.0f);

        static SpriteAlphaMask opaqueMask() {
            return FULLY_OPAQUE;
        }

        static SpriteAlphaMask transparentMask() {
            return FULLY_TRANSPARENT;
        }
    }

    private static final class LocalTexturedQuad {
        final Vec3[] worldVertices;
        final AtlasUv[] atlasUvs;

        private LocalTexturedQuad(Vec3[] worldVertices, AtlasUv[] atlasUvs) {
            this.worldVertices = worldVertices;
            this.atlasUvs = atlasUvs;
        }

        @Nullable
        static LocalTexturedQuad from(BakedQuad quad) {
            int[] vertices = quad.m_111303_();
            if (vertices.length < 32) {
                return null;
            }
            Vec3[] worldVertices = new Vec3[4];
            AtlasUv[] atlasUvs = new AtlasUv[4];
            for (int vertexIndex = 0; vertexIndex < 4; ++vertexIndex) {
                int baseIndex = vertexIndex * 8;
                double x = Float.intBitsToFloat(vertices[baseIndex]);
                double y = Float.intBitsToFloat(vertices[baseIndex + 1]);
                double z = Float.intBitsToFloat(vertices[baseIndex + 2]);
                double u = Float.intBitsToFloat(vertices[baseIndex + 4]);
                double v = Float.intBitsToFloat(vertices[baseIndex + 5]);
                worldVertices[vertexIndex] = new Vec3(x, y, z);
                atlasUvs[vertexIndex] = new AtlasUv(u, v);
            }
            return new LocalTexturedQuad(worldVertices, atlasUvs);
        }
    }

    private record SpritePixelBounds(double minX, double maxX, double minY, double maxY) {
    }

    private record SpriteRect(int minX, int maxX, int minY, int maxY) {
    }

    private record AtlasUv(double u, double v) {
    }
}
