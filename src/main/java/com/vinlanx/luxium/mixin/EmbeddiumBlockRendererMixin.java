/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.model.quad.BakedQuadView
 *  me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing
 *  me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers
 *  me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material
 *  me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder$Vertex
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.BlockGetter
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.plantswave.PlantWaveMaterialEncoder;
import com.vinlanx.luxium.client.plantswave.PlantWaveProfile;
import com.vinlanx.luxium.client.plantswave.PlantWaveRegistry;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"})
public abstract class EmbeddiumBlockRendererMixin {
    @Unique
    private Direction luxium$quadNormalDirection;
    @Unique
    private boolean luxium$quadNormalAxisAligned;
    @Unique
    private BakedQuadView luxium$quad;
    @Unique
    private PlantWaveProfile luxium$plantWaveProfile = PlantWaveProfile.NONE;
    @Unique
    private int luxium$plantWavePhase;

    @Inject(method={"renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V"}, at={@At(value="HEAD")}, remap=false, require=1)
    private void luxium$preparePlantWave(BlockRenderContext context, ChunkBuildBuffers buffers, CallbackInfo ci) {
        this.luxium$quad = null;
        this.luxium$plantWaveProfile = PlantWaveRegistry.resolve((BlockGetter)context.localSlice(), context.pos(), context.state());
        this.luxium$plantWavePhase = EmbeddiumBlockRendererMixin.luxium$phase(context.pos().m_123341_(), context.pos().m_123343_());
    }

    @Redirect(method={"writeGeometry"}, at=@At(value="INVOKE", target="Lme/jellysquid/mods/sodium/client/model/quad/BakedQuadView;getNormalFace()Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;"), remap=false, require=1)
    private ModelQuadFacing luxium$captureQuadNormal(BakedQuadView quad) {
        this.luxium$quad = quad;
        ModelQuadFacing facing = quad.getNormalFace();
        this.luxium$quadNormalDirection = quad.getLightFace();
        this.luxium$quadNormalAxisAligned = this.luxium$quadNormalDirection != null;
        return facing;
    }

    @ModifyArg(method={"writeGeometry"}, at=@At(value="INVOKE", target="Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V"), index=0, remap=false, require=1)
    private ChunkVertexEncoder.Vertex[] luxium$encodePlantWaveWeights(ChunkVertexEncoder.Vertex[] vertices) {
        BakedQuadView quad = this.luxium$quad;
        PlantWaveProfile profile = this.luxium$plantWaveProfile;
        if (quad == null || profile == PlantWaveProfile.NONE) {
            return vertices;
        }
        int count = Math.min(vertices.length, 4);
        for (int i = 0; i < count; ++i) {
            int weight = profile.vertexWeight(quad.getY(i));
            int packed = (this.luxium$plantWavePhase & 0xF) << 4 | weight & 0xF;
            vertices[i].color = vertices[i].color & 0xFFFFFF | packed << 24;
        }
        return vertices;
    }

    @ModifyArg(method={"writeGeometry"}, at=@At(value="INVOKE", target="Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V"), index=1, remap=false, require=1)
    private Material luxium$encodeQuadMaterial(Material material) {
        return PlantWaveMaterialEncoder.encode(material, this.luxium$quadNormalDirection, this.luxium$quadNormalAxisAligned, this.luxium$plantWaveProfile);
    }

    @Unique
    private static int luxium$phase(int x, int z) {
        int h = x * 0x1F1F1F1F ^ z * 1597334677;
        h ^= h >>> 16;
        h *= 2146121005;
        h ^= h >>> 15;
        return h & 0xF;
    }
}

