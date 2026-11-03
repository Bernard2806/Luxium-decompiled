package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.plantswave.PlantWaveMaterialEncoder;
import com.vinlanx.luxium.client.plantswave.PlantWaveProfile;
import com.vinlanx.luxium.client.plantswave.PlantWaveRegistry;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder.Vertex;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets = {"me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"}, remap = false ) public abstract

 class SodiumBlockRendererMixin {
    @Unique private Direction luxium$quadNormalDirection; @Unique private boolean luxium$quadNormalAxisAligned; @Unique private BakedQuadView luxium$quad; @Unique private ModelQuadOrientation luxium$quadOrientation;



 @Unique private PlantWaveProfile luxium$plantWaveProfile = PlantWaveProfile.NONE;
    @Unique private int luxium$plantWavePhase;

    @Inject(
        method = {"renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V"},
        at = {@At("HEAD")},
        remap = false, require = 1 ) private void luxium$preparePlantWave(BlockRenderContext context, ChunkBuildBuffers buffers, CallbackInfo ci) {



 this.luxium$quad = null;
        this.luxium$plantWaveProfile = PlantWaveRegistry.resolve(context.world(), context.pos(), context.state());
        this.luxium$plantWavePhase = luxium$phase(context.pos().getX(), context.pos().getZ());
    }

    @Inject(
        method = {"writeGeometry"},
        at = {@At("HEAD")},
        remap = false,
        require = 1 ) private void luxium$captureQuad(
        BlockRenderContext context,
        ChunkModelBuilder builder, Vec3 offset, Material material, BakedQuadView quad, int[] colors, QuadLightData light, CallbackInfo ci ) {


 this.luxium$quad = quad;
        this.luxium$quadOrientation = ModelQuadOrientation.orientByBrightness(light.br, light.lm);
        ModelQuadFacing normalFace = quad.getNormalFace();
        this.luxium$quadNormalDirection = switch (normalFace) {
            case POS_X -> Direction.EAST;
            case POS_Y -> Direction.UP;
            case POS_Z -> Direction.SOUTH;
            case NEG_X -> Direction.WEST;
            case NEG_Y -> Direction.DOWN;
            case NEG_Z -> Direction.NORTH;
            case UNASSIGNED -> null;
        };
        this.luxium$quadNormalAxisAligned = normalFace != ModelQuadFacing.UNASSIGNED;
    }

    @ModifyArg(
        method = {"writeGeometry"},
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V",
            remap = false ),
        index = 0,
        remap = false,
        require = 1 ) private Vertex[] luxium$encodePlantWaveWeights(Vertex[] vertices) {
        PlantWaveProfile


        profile = this.luxium$plantWaveProfile;
        if (profile == PlantWaveProfile.NONE) {
            return vertices;
        } else {
            for (int
            dstIndex = 0; dstIndex < 4; dstIndex++) {
                int srcIndex = this.luxium$quadOrientation.getVertexIndex(dstIndex);
                int weight = profile.vertexWeight(this.luxium$quad.getY(srcIndex));
                int packed = (this.luxium$plantWavePhase & 15) << 4 | weight & 15;
                int color = vertices[dstIndex].color;
                int brightness = ColorABGR.unpackAlpha(color);
                vertices[dstIndex].color = ColorABGR.pack(
                    luxium$scaleColorChannel(ColorABGR.unpackRed(color), brightness),
                    luxium$scaleColorChannel(ColorABGR.unpackGreen(color), brightness),
                    luxium$scaleColorChannel(ColorABGR.unpackBlue(color), brightness),
                    packed );
            }
            return vertices;
        }
    }

    @ModifyArg(
        method = {"writeGeometry"},
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V",
            remap = false ),
        index = 1,
        remap = false,
        require = 1 ) private Material luxium$encodeQuadMaterial(Material material) {


 return PlantWaveMaterialEncoder.encode(material, this.luxium$quadNormalDirection, this.luxium$quadNormalAxisAligned, this.luxium$plantWaveProfile);
    }

    @Unique private static int luxium$scaleColorChannel(int channel, int brightness) {
        int





        product = channel * brightness + 128;
        return product + (product >>> 8) >>> 8;
    }

    @Unique private static int luxium$phase(int x, int z) {
        int
        h = x * 522133279 ^ z * 1597334677;
        h ^= h >>> 16;
        h *= 2146121005;
        h ^= h >>> 15;
        return h & 15;
    }
    }
