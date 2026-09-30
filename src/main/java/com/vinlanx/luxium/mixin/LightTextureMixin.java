/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.dimension.DimensionType
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
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

@Mixin(value={LightTexture.class})
public abstract class LightTextureMixin
implements NeoSkyLightTextureExtension {
    @Shadow
    private boolean f_109873_;
    @Shadow(aliases={"f_109871_"})
    @Final
    private NativeImage f_109871_;
    @Shadow
    private float f_109874_;
    @Shadow
    @Final
    private GameRenderer f_109875_;
    @Shadow
    @Final
    private Minecraft f_109876_;
    @Unique
    private DynamicTexture luxium$blockOnlyTexture;
    @Unique
    private NativeImage luxium$blockOnlyPixels;
    @Unique
    private boolean luxium$lightmapWasDirty;
    @Unique
    private long luxium$lightmapRevision;

    @Shadow
    private float m_234319_(float partialTick) {
        throw new AssertionError();
    }

    @Shadow
    private float m_234312_(LivingEntity entity, float darknessGamma, float partialTick) {
        throw new AssertionError();
    }

    @Shadow
    private float m_109892_(float value) {
        throw new AssertionError();
    }

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void luxium$createBlockOnlyLightmap(GameRenderer renderer, Minecraft minecraft, CallbackInfo ci) {
        this.luxium$blockOnlyTexture = new DynamicTexture(16, 16, false);
        minecraft.m_91097_().m_118490_("luxium_block_only_light_map", this.luxium$blockOnlyTexture);
        this.luxium$blockOnlyPixels = this.luxium$blockOnlyTexture.m_117991_();
        for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
            for (int blockLevel = 0; blockLevel < 16; ++blockLevel) {
                this.luxium$blockOnlyPixels.m_84988_(blockLevel, skyLevel, -1);
            }
        }
        this.luxium$blockOnlyTexture.m_117985_();
        LightTextureMixin.luxium$configureLinearLightmapTexture(this.luxium$blockOnlyTexture.m_117963_());
    }

    @Inject(method={"updateLightTexture"}, at={@At(value="HEAD")})
    private void luxium$captureLightmapDirty(float partialTick, CallbackInfo ci) {
        this.luxium$lightmapWasDirty = this.f_109873_;
    }

    @Inject(method={"updateLightTexture"}, at={@At(value="TAIL")})
    private void luxium$updateBlockOnlyLightmap(float partialTick, CallbackInfo ci) {
        if (this.luxium$lightmapWasDirty) {
            this.luxium$rebuildBlockOnlyLightmap(partialTick);
            ++this.luxium$lightmapRevision;
        }
    }

    @Inject(method={"close"}, at={@At(value="TAIL")})
    private void luxium$closeBlockOnlyLightmap(CallbackInfo ci) {
        if (this.luxium$blockOnlyTexture != null) {
            this.luxium$blockOnlyTexture.close();
            this.luxium$blockOnlyTexture = null;
            this.luxium$blockOnlyPixels = null;
        }
    }

    @Override
    public int neosky$getBlockOnlyLightTextureId() {
        return this.luxium$blockOnlyTexture != null ? this.luxium$blockOnlyTexture.m_117963_() : -1;
    }

    @Override
    public int neosky$getMainLightPixel(int blockLevel, int skyLevel) {
        if (this.f_109871_ == null) {
            return -1;
        }
        return this.f_109871_.m_84985_(Mth.m_14045_((int)blockLevel, (int)0, (int)15), Mth.m_14045_((int)skyLevel, (int)0, (int)15));
    }

    @Override
    public int neosky$getBlockOnlyLightPixel(int blockLevel, int skyLevel) {
        if (this.luxium$blockOnlyPixels == null) {
            return -1;
        }
        return this.luxium$blockOnlyPixels.m_84985_(Mth.m_14045_((int)blockLevel, (int)0, (int)15), Mth.m_14045_((int)skyLevel, (int)0, (int)15));
    }

    @Override
    public long neosky$getLightmapRevision() {
        return this.luxium$lightmapRevision;
    }

    @ModifyArg(method={"updateLightTexture"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", ordinal=1), index=1)
    private int luxium$forceSkyOnlyBlockLight(int blockLevel) {
        return NeoShadowsEngine.isAnyShadowCapturePass() ? 0 : blockLevel;
    }

    @Redirect(method={"updateLightTexture"}, at=@At(value="INVOKE", target="Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", remap=false))
    private Vector3f luxium$colorizeBlockLight(Vector3f vector, float red, float green, float blue) {
        LightTextureMixin.luxium$setColoredBlockLight(vector, red, green, blue);
        return vector;
    }

    @Unique
    private void luxium$rebuildBlockOnlyLightmap(float partialTick) {
        ClientLevel level = this.f_109876_.f_91073_;
        if (level == null || this.f_109876_.f_91074_ == null || this.luxium$blockOnlyPixels == null || this.luxium$blockOnlyTexture == null) {
            return;
        }
        float skyDarken = level.m_104805_(1.0f);
        float skyBrightnessScale = level.m_104819_() > 0 ? 1.0f : skyDarken * 0.95f + 0.05f;
        float darknessEffectScale = ((Double)this.f_109876_.f_91066_.m_231926_().m_231551_()).floatValue();
        float darknessGamma = this.m_234319_(partialTick) * darknessEffectScale;
        float darknessScale = this.m_234312_((LivingEntity)this.f_109876_.f_91074_, darknessGamma, partialTick) * darknessEffectScale;
        float waterVision = this.f_109876_.f_91074_.m_108639_();
        float nightVision = this.f_109876_.f_91074_.m_21023_(MobEffects.f_19611_) ? GameRenderer.m_109108_((LivingEntity)this.f_109876_.f_91074_, (float)partialTick) : (waterVision > 0.0f && this.f_109876_.f_91074_.m_21023_(MobEffects.f_19592_) ? waterVision : 0.0f);
        Vector3f skyColor = new Vector3f(skyDarken, skyDarken, 1.0f).lerp((Vector3fc)new Vector3f(1.0f, 1.0f, 1.0f), 0.35f);
        float blockScale = this.f_109874_ + 1.5f;
        Vector3f color = new Vector3f();
        for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
            for (int blockLevel = 0; blockLevel < 16; ++blockLevel) {
                float max;
                float skyBrightness = LightTexture.m_234316_((DimensionType)level.m_6042_(), (int)0) * skyBrightnessScale;
                float blockBrightness = LightTexture.m_234316_((DimensionType)level.m_6042_(), (int)blockLevel) * blockScale;
                float green = blockBrightness * ((blockBrightness * 0.6f + 0.4f) * 0.6f + 0.4f);
                float blue = blockBrightness * (blockBrightness * blockBrightness * 0.6f + 0.4f);
                LightTextureMixin.luxium$setColoredBlockLight(color, blockBrightness, green, blue);
                boolean forceBright = level.m_104583_().m_108884_();
                if (forceBright) {
                    color.lerp((Vector3fc)new Vector3f(0.99f, 1.12f, 1.0f), 0.25f);
                    LightTextureMixin.luxium$clampColor(color);
                } else {
                    color.add((Vector3fc)new Vector3f((Vector3fc)skyColor).mul(skyBrightness));
                    color.lerp((Vector3fc)new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
                    float darkenWorld = this.f_109875_.m_109131_(partialTick);
                    if (darkenWorld > 0.0f) {
                        color.lerp((Vector3fc)new Vector3f((Vector3fc)color).mul(0.7f, 0.6f, 0.6f), darkenWorld);
                    }
                }
                level.m_104583_().adjustLightmapColors(level, partialTick, skyDarken, blockScale, skyBrightness, blockLevel, skyLevel, color);
                if (nightVision > 0.0f && (max = Math.max(color.x(), Math.max(color.y(), color.z()))) > 1.0E-5f && max < 1.0f) {
                    color.lerp((Vector3fc)new Vector3f((Vector3fc)color).mul(1.0f / max), nightVision);
                }
                if (!forceBright) {
                    if (darknessScale > 0.0f) {
                        color.add(-darknessScale, -darknessScale, -darknessScale);
                    }
                    LightTextureMixin.luxium$clampColor(color);
                }
                float gamma = ((Double)this.f_109876_.f_91066_.m_231927_().m_231551_()).floatValue();
                Vector3f gammaColor = new Vector3f(this.m_109892_(color.x()), this.m_109892_(color.y()), this.m_109892_(color.z()));
                color.lerp((Vector3fc)gammaColor, Math.max(0.0f, gamma - darknessGamma));
                color.lerp((Vector3fc)new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
                LightTextureMixin.luxium$clampColor(color);
                color.mul(255.0f);
                int red = (int)color.x();
                int greenChannel = (int)color.y();
                int blueChannel = (int)color.z();
                this.luxium$blockOnlyPixels.m_84988_(blockLevel, skyLevel, 0xFF000000 | blueChannel << 16 | greenChannel << 8 | red);
            }
        }
        this.luxium$blockOnlyTexture.m_117985_();
    }

    @Unique
    private static void luxium$setColoredBlockLight(Vector3f out, float red, float green, float blue) {
        if (!Coloredlight.isEnabled() || !Coloredlight.isWorldRenderActive()) {
            out.set(red, green, blue);
            return;
        }
        float intensity = Math.max(red, Math.max(green, blue));
        out.set(intensity * Coloredlight.getRedMultiplier(), intensity * Coloredlight.getGreenMultiplier(), intensity * Coloredlight.getBlueMultiplier());
    }

    @Unique
    private static void luxium$clampColor(Vector3f color) {
        color.set(Mth.m_14036_((float)color.x(), (float)0.0f, (float)1.0f), Mth.m_14036_((float)color.y(), (float)0.0f, (float)1.0f), Mth.m_14036_((float)color.z(), (float)0.0f, (float)1.0f));
    }

    @Unique
    private static void luxium$configureLinearLightmapTexture(int textureId) {
        int oldActive = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture((int)33984);
        GlStateManager._bindTexture((int)textureId);
        RenderSystem.texParameter((int)3553, (int)10241, (int)9729);
        RenderSystem.texParameter((int)3553, (int)10240, (int)9729);
        RenderSystem.texParameter((int)3553, (int)10242, (int)33071);
        RenderSystem.texParameter((int)3553, (int)10243, (int)33071);
        GlStateManager._activeTexture((int)oldActive);
    }
}

