package com.vinlanx.luxium.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.guiscreen.IncompatibleModScreen;
import com.vinlanx.luxium.platform.config.ConfigSpec;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class LuxiumPlatform {
    private static LuxiumPlatform instance;
    private static IncompatibleModScreen incompatibilityScreen;

    public static void initialize(LuxiumPlatform platform) {
        if (instance != null) { throw new IllegalStateException("Luxium platform is already initialized");
        } else { instance = Objects.requireNonNull(platform);
            incompatibilityScreen = platform.createIncompatibilityScreen();
        } }

    public static IncompatibleModScreen incompatibilityScreen() {
        return incompatibilityScreen;
    }

    public static LuxiumPlatform get() {
        return Objects.requireNonNull(instance, "Luxium platform has not been initialized");
    }

    public abstract void registerConfig(ConfigSpec var1);

    protected abstract IncompatibleModScreen createIncompatibilityScreen();

    public abstract boolean isModLoaded(String var1);

    public abstract boolean externalShadersLoaded();

    public abstract String version();

    public abstract int lightEmission(BlockState var1, BlockGetter var2, BlockPos var3);

    public abstract float partialTick(Minecraft var1);

    public abstract AABB blockEntityRenderBounds(BlockEntity var1);

    public abstract int spriteFirstFramePixel(TextureAtlasSprite var1, int var2, int var3);

    public abstract void renderParticles(ParticleEngine var1, PoseStack var2, BufferSource var3, LightTexture var4, Camera var5, float var6, Frustum var7);
 }
