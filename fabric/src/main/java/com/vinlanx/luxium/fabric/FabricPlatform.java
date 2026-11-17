package com.vinlanx.luxium.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.guiscreen.IncompatibleModScreen;
import com.vinlanx.luxium.client.guiscreen.NOiris;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import com.vinlanx.luxium.mixin.SpriteContentsAccessor;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import com.vinlanx.luxium.platform.config.ConfigSpec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final
class FabricPlatform extends LuxiumPlatform {
    private FabricConfig config;

    @Override public void registerConfig(ConfigSpec spec) {
 if (this.config != null) { throw new IllegalStateException("Luxium config is already registered");
        } else { this.config = new FabricConfig(FabricLoader.getInstance().getConfigDir().resolve("luxium-client.toml"), spec);
        } }
    public void close() { this.config.close(); }

    @Override protected IncompatibleModScreen createIncompatibilityScreen() { if (FabricLoader.getInstance().isModLoaded("iris")) { return new NOiris(); }
 return null; } @Override public boolean isModLoaded(String id) {

 return FabricLoader.getInstance().isModLoaded(id); } @Override public boolean externalShadersLoaded() {
 return FabricLoader.getInstance().isModLoaded("iris");
    } @Override public String version() {
 return FabricLoader.getInstance().getModContainer("luxium").orElseThrow().getMetadata().getVersion().getFriendlyString();
    }
    @Override public int lightEmission(BlockState state, BlockGetter level, BlockPos pos) {
 return state.getLightEmission();
    }
    @Override public float partialTick(Minecraft client) {
 return client.isPaused() ? ((MinecraftAccessor)client).luxium$getPausePartialTick() : client.getFrameTime
 ();
    }
    @Override public int spriteFirstFramePixel(TextureAtlasSprite sprite, int x, int y) {
 return ((SpriteContentsAccessor)sprite.contents()).luxium$getOriginalImage().getPixelRGBA(x, y);
    }

    @Override public void renderParticles(ParticleEngine engine, PoseStack poseStack, BufferSource buffers, LightTexture lightTexture, Camera camera, float partialTick, Frustum frustum ) {

 engine.render(poseStack, buffers, lightTexture, camera, partialTick);
    }

    @Override public AABB blockEntityRenderBounds(BlockEntity entity) { BlockPos pos = entity.getBlockPos();
        if (entity instanceof SignBlockEntity) { return new AABB(pos);
        } BlockState state = entity.getBlockState();
            Block block = state.getBlock();
            if (block == Blocks.ENCHANTING_TABLE) { return new AABB(pos, pos.offset(1, 1, 1));
            } else if (!(block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST)) { return new AABB(pos.offset(-1, 0, -1), pos.offset(2, 2, 2));
            } if (block != Blocks.STRUCTURE_BLOCK && block != Blocks.BEACON) {
                    VoxelShape shape = state.getCollisionShape(entity.getLevel(), pos);
                    if (!shape.isEmpty()) { return shape.bounds().move(pos);
                    }
                } return new AABB(
                    Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY );

        
    }
    }
