package com.vinlanx.luxium.mixin;




import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.shadows.neoskycelestia.lightmap.NeoSkyLightTextureExtension;
import com.vinlanx.luxium.rtx.Coloredlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={LightTexture.class}) public abstract
class LightTextureMixin implements NeoSkyLightTextureExtension { @Shadow private boolean updateLightTexture; /* Split so the SRG-to-official source remap keeps the literal SRG alias of the original jar. */ @Shadow(aliases={"f_" + "109871_"}) @Final private NativeImage lightPixels; @Shadow private float blockLightRedFlicker; @Shadow @Final private GameRenderer renderer; @Shadow @Final private Minecraft minecraft; @Unique private DynamicTexture luxium$blockOnlyTexture; @Unique private NativeImage luxium$blockOnlyPixels;
    @Unique private boolean luxium$lightmapWasDirty;
    @Unique private long luxium$lightmapRevision;
    @Shadow private float getDarknessGamma(float partialTick) {
        throw new AssertionError(); } @Shadow private float calculateDarknessScale(LivingEntity entity, float darknessGamma, float partialTick) {
        throw new AssertionError(); } @Shadow private float notGamma(float value) {
        throw new AssertionError();
    }





    @Inject(method={"<init>"}, at={@At(value="RETURN")}) private void luxium$createBlockOnlyLightmap(GameRenderer renderer, Minecraft minecraft, CallbackInfo ci) {




        this.luxium$blockOnlyTexture = new DynamicTexture(16, 16, false);
        minecraft.getTextureManager().register("luxium_block_only_light_map", this.luxium$blockOnlyTexture);
        this.luxium$blockOnlyPixels = this.luxium$blockOnlyTexture.getPixels();

        for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
            for (int blockLevel = 0; blockLevel < 16; ++blockLevel) {
                this.luxium$blockOnlyPixels.setPixelRGBA(blockLevel, skyLevel, -1);
            }
        }

        this.luxium$blockOnlyTexture.upload();
        LightTextureMixin.luxium$configureLinearLightmapTexture(this.luxium$blockOnlyTexture.getId());
    }

    @Inject(method={"updateLightTexture"}, at={@At(value="HEAD")}) private void luxium$captureLightmapDirty(float partialTick, CallbackInfo ci) {

        this.luxium$lightmapWasDirty = this.updateLightTexture;
    }

    @Inject(method={"updateLightTexture"}, at={@At(value="TAIL")}) private void luxium$updateBlockOnlyLightmap(float partialTick, CallbackInfo ci) {

        if (this.luxium$lightmapWasDirty) {
            this.luxium$rebuildBlockOnlyLightmap(partialTick);
            ++this.luxium$lightmapRevision;
        }
    }

    @Inject(method={"close"}, at={@At(value="TAIL")}) private void luxium$closeBlockOnlyLightmap(CallbackInfo ci) {

        if (this.luxium$blockOnlyTexture != null) {
            this.luxium$blockOnlyTexture.close();
            this.luxium$blockOnlyTexture = null;
            this.luxium$blockOnlyPixels = null;
        }
    }

    @Override public int neosky$getBlockOnlyLightTextureId() {

        return this.luxium$blockOnlyTexture != null ? this.luxium$blockOnlyTexture.getId() : -1;
    }

    @Override public int neosky$getMainLightPixel(int blockLevel, int skyLevel) {

        if (this.lightPixels == null) {
            return -1;
        }
        return this.lightPixels.getPixelRGBA(Mth.clamp
                ((int)blockLevel, (int)0, (int)15), Mth.clamp
                ((int)skyLevel, (int)0, (int)15));
    }

    @Override public int neosky$getBlockOnlyLightPixel(int blockLevel, int skyLevel) {

        if (this.luxium$blockOnlyPixels == null) {
            return -1;
        }
        return this.luxium$blockOnlyPixels.getPixelRGBA(Mth.clamp
                ((int)blockLevel, (int)0, (int)15), Mth.clamp
                ((int)skyLevel, (int)0, (int)15));
    }

    @Override public long neosky$getLightmapRevision() {

        return this.luxium$lightmapRevision;
    }









    @ModifyArg(method={"updateLightTexture"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", ordinal=1), index=1) private int luxium$forceSkyOnlyBlockLight(int blockLevel) {

        return NeoShadowsEngine.isAnyShadowCapturePass() ? 0 : blockLevel;
    }








    @Redirect(method={"updateLightTexture"}, at=@At(value="INVOKE", target="Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", remap=false)) private Vector3f luxium$colorizeBlockLight(Vector3f vector, float red, float green, float blue) {

        LightTextureMixin.luxium$setColoredBlockLight(vector, red, green, blue);
        return vector;
    }

    @Unique private void luxium$rebuildBlockOnlyLightmap(float partialTick) {
        ClientLevel
        level = this.minecraft.level;
        if (level == null || this.minecraft.player == null || this.luxium$blockOnlyPixels == null || this.luxium$blockOnlyTexture == null) {

            return;
        }

        float skyDarken = level.getSkyDarken(1.0f);
        float skyBrightnessScale = level.getSkyFlashTime() > 0 ? 1.0f : skyDarken * 0.95f + 0.05f;
        float darknessEffectScale = ((Double)this.minecraft.options.darknessEffectScale().get()).floatValue();
        float darknessGamma = this.getDarknessGamma(partialTick) * darknessEffectScale;
        float darknessScale = this.calculateDarknessScale((LivingEntity)this.minecraft.player, darknessGamma, partialTick) * darknessEffectScale;

        float waterVision = this.minecraft.player.getWaterVision();
        float nightVision;
        if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
            nightVision = GameRenderer.getNightVisionScale((LivingEntity)this.minecraft.player, (float)partialTick);
        } else if (waterVision > 0.0f && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
            nightVision = waterVision;
        } else {
            nightVision = 0.0f;
        }

        Vector3f skyColor = new Vector3f(skyDarken, skyDarken, 1.0f).lerp
                ((Vector3fc)new Vector3f(1.0f, 1.0f, 1.0f), 0.35f);
        float blockScale = this.blockLightRedFlicker + 1.5f;
        Vector3f color = new Vector3f();

        for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
            for (int blockLevel = 0; blockLevel < 16; ++blockLevel) {
                float skyBrightness = LightTexture.getBrightness((DimensionType)level.dimensionType(), (int)0) * skyBrightnessScale;
                float blockBrightness = LightTexture.getBrightness((DimensionType)level.dimensionType(), (int)blockLevel) * blockScale;
                float green = blockBrightness * ((blockBrightness * 0.6f + 0.4f) * 0.6f + 0.4f);
                float blue = blockBrightness * (blockBrightness * blockBrightness * 0.6f + 0.4f);
                LightTextureMixin.luxium$setColoredBlockLight(color, blockBrightness, green, blue);

                boolean forceBright = level.effects().forceBrightLightmap();
                if (forceBright) {
                    color.lerp((Vector3fc)new Vector3f(0.99f, 1.12f, 1.0f), 0.25f);
                    LightTextureMixin.luxium$clampColor(color);
                } else {
                    color.add((Vector3fc)new Vector3f((Vector3fc)skyColor).mul(skyBrightness));
                    color.lerp((Vector3fc)new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
                    float darkenWorld = this.renderer.getDarkenWorldAmount(partialTick);
                    if (darkenWorld > 0.0f) {
                        color.lerp((Vector3fc)new Vector3f((Vector3fc)color).mul(0.7f, 0.6f, 0.6f), darkenWorld);
                    }
                }

                level.effects().adjustLightmapColors(level, partialTick, skyDarken, blockScale, skyBrightness, blockLevel, skyLevel, color);


                if (nightVision > 0.0f) {
                    float max = Math.max(color.x(), Math.max(color.y(), color.z()));
                    if (max > 1.0E-5f && max < 1.0f) {
                        color.lerp((Vector3fc)new Vector3f((Vector3fc)color).mul(1.0f / max), nightVision);
                    }
                }

                if (!forceBright) {
                    if (darknessScale > 0.0f) {
                        color.add(-darknessScale, -darknessScale, -darknessScale);
                    }
                    LightTextureMixin.luxium$clampColor(color);
                }

                float gamma = ((Double)this.minecraft.options.gamma().get()).floatValue();
                Vector3f gammaColor = new Vector3f(this.notGamma
                        (color.x()), this.notGamma(color.y()), this.notGamma(color.z()));
                color.lerp((Vector3fc)gammaColor, Math.max(0.0f, gamma - darknessGamma));
                color.lerp((Vector3fc)new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
                LightTextureMixin.luxium$clampColor(color);
                color.mul(255.0f);

                int red = (int)color.x();
                int greenChannel = (int)color.y();
                int blueChannel = (int)color.z();
                this.luxium$blockOnlyPixels.setPixelRGBA(blockLevel, skyLevel, 0xFF000000 | blueChannel << 16 | greenChannel << 8 | red);
            }
        }


        this.luxium$blockOnlyTexture.upload();
    }

    @Unique private static void luxium$setColoredBlockLight(Vector3f out, float red, float green, float blue) {
        if (!Coloredlight.isEnabled
() || !Coloredlight.isWorldRenderActive()) {
            out.set(red, green, blue);
            return;
        }

        float intensity = Math.max(red, Math.max(green, blue));
        out.set(intensity * Coloredlight.getRedMultiplier
                (), intensity * Coloredlight.getGreenMultiplier
                (), intensity * Coloredlight.getBlueMultiplier
                ());

    }

    @Unique private static void luxium$clampColor(Vector3f color) {

        color.set(Mth.clamp
                ((float)color.x(), (float)0.0f, (float)1.0f), Mth.clamp
                ((float)color.y(), (float)0.0f, (float)1.0f), Mth.clamp
                ((float)color.z(), (float)0.0f, (float)1.0f));
    }

    @Unique private static void luxium$configureLinearLightmapTexture(int textureId) {
        int oldActive = GlStateManager._getActiveTexture
 ();
        GlStateManager._activeTexture((int)33984);
        GlStateManager._bindTexture((int)textureId);
        RenderSystem.texParameter((int)3553, (int)10241, (int)9729);
        RenderSystem.texParameter((int)3553, (int)10240, (int)9729);
        RenderSystem.texParameter((int)3553, (int)10242, (int)33071);
        RenderSystem.texParameter((int)3553, (int)10243, (int)33071);
        GlStateManager._activeTexture((int)oldActive);
    }
    }
