package com.vinlanx.luxium.forge;

import com.mojang.blaze3d.vertex.PoseStack; import com.vinlanx.luxium.client.guiscreen.IncompatibleModScreen; import com.vinlanx.luxium.client.guiscreen.NOoculus; import com.vinlanx.luxium.platform.LuxiumPlatform; import com.vinlanx.luxium.platform.config.ConfigSpec; import net.minecraft.client.Camera; import net.minecraft.client.Minecraft; import net.minecraft.client.particle.ParticleEngine; import net.minecraft.client.renderer.LightTexture; import net.minecraft.client.renderer.MultiBufferSource.BufferSource; import net.minecraft.client.renderer.culling.Frustum; import net.minecraft.client.renderer.texture.TextureAtlasSprite; import net.minecraft.core.BlockPos; import net.minecraft.world.level.BlockGetter; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.phys.AABB; import net.minecraftforge.common.ForgeConfigSpec; import net.minecraftforge.common.ForgeConfigSpec.Builder; import net.minecraftforge.common.ForgeConfigSpec.ConfigValue; import net.minecraftforge.fml.ModContainer; import net.minecraftforge.fml.ModList; import net.minecraftforge.fml.ModLoadingContext; import net.minecraftforge.fml.config.ModConfig.Type;



















public final class ForgePlatform extends LuxiumPlatform {

    @Override public void registerConfig(ConfigSpec spec) { Builder builder = new Builder();
        for (ConfigSpec.ConfigValue<?> value : spec.values()) {
            if (!value.comment().isEmpty()) { builder.comment(value.comment().split("\n"));
            } if (value instanceof ConfigSpec.BooleanValue bool) {
                bind(bool, builder.define(bool.path(), bool.getDefault()));
            } else if (value instanceof ConfigSpec.IntValue integer) {
                bind(integer, builder.defineInRange(integer.path(), integer.getDefault(), integer.min(), integer.max()));
            } else if (value instanceof ConfigSpec.DoubleValue decimal) {
                bind(decimal, builder.defineInRange(decimal.path(), decimal.getDefault(), decimal.min(), decimal.max()));
            } else if (value instanceof ConfigSpec.EnumValue<?> enumeration) {
                bindEnum(builder, enumeration);
            } else {
                throw new IllegalArgumentException("Unsupported config value: " + value.path()); }

} ForgeConfigSpec
nativeSpec = builder.build();
spec.bindSave(nativeSpec::save); ModLoadingContext.get
 ().registerConfig(Type.CLIENT, nativeSpec, "luxium-client.toml");
 } private static <T> void bind(ConfigSpec.ConfigValue<T> value, ConfigValue<T> nativeValue) {


 value.bind(nativeValue::get, nativeValue::set, nativeValue::save);
    }

    private static <E extends Enum<E>> void bindEnum(Builder builder, ConfigSpec.EnumValue<E> value) {
        bind(value, builder.defineEnum(value.path(), value.getDefault(), value.choices()));
    }
    @Override public boolean isModLoaded(String id) {
 return ModList.get().isLoaded(id);
    }
    @Override protected IncompatibleModScreen createIncompatibilityScreen() { if (ModList.get().isLoaded("oculus")) { return new NOoculus(); }
 return null; } @Override public boolean externalShadersLoaded() {

 return ModList.get().isLoaded("oculus");
    } @Override public String version() {
 return ((ModContainer)ModList.get().getModContainerById("luxium").orElseThrow()).getModInfo().getVersion().toString();
    }
    @Override public int lightEmission(BlockState state, BlockGetter level, BlockPos pos) {
 return state.getLightEmission(level, pos);
    } @Override public float partialTick(Minecraft client) {
 return client.getPartialTick();
    } @Override public AABB blockEntityRenderBounds(BlockEntity entity) {
 return entity.getRenderBoundingBox();
    }
    @Override public int spriteFirstFramePixel(TextureAtlasSprite sprite, int x, int y) {
 return sprite.getPixelRGBA(0, x, y);
    }

    @Override public void renderParticles(ParticleEngine engine, PoseStack poseStack, BufferSource buffers, LightTexture lightTexture, Camera camera, float partialTick, Frustum frustum ) {

 engine.render(poseStack, buffers, lightTexture, camera, partialTick, frustum);
    }
    }
