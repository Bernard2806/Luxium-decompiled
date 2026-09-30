/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Gui.class})
public abstract class GuiMixin {
    @Shadow
    public abstract void m_280154_(GuiGraphics var1, Entity var2);

    private static boolean luxium$hasUnsafePostEffects() {
        return Config.isFeatureEnabled(Config.CLIENT.skyGodRaysEnabled) || Config.isFeatureEnabled(Config.CLIENT.lensFlareEnabled) || Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled);
    }

    private static boolean luxium$shouldCancelVignette() {
        return PostProcessStateGuard.unsafeGuiCompositeStateSeenThisSession() || PostProcessStateGuard.repairedGuiCompositeStateThisFrame() || GuiMixin.luxium$hasUnsafePostEffects();
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/Gui;renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V"), require=0)
    private void luxium$conditionallyRenderVignette(Gui instance, GuiGraphics guiGraphics, Entity entity) {
        if (GuiMixin.luxium$shouldCancelVignette()) {
            PostProcessStateGuard.restoreGuiCompositeState();
            return;
        }
        this.m_280154_(guiGraphics, entity);
    }

    @Inject(method={"render"}, at={@At(value="HEAD")}, require=0)
    private void luxium$resetGuiRenderState(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState();
    }

    @Inject(method={"renderVignette"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void luxium$resetVignetteRenderState(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (GuiMixin.luxium$shouldCancelVignette()) {
            ci.cancel();
            return;
        }
        PostProcessStateGuard.restoreGuiCompositeState();
        Minecraft mc = Minecraft.m_91087_();
        RenderSystem.viewport((int)0, (int)0, (int)mc.m_91268_().m_85441_(), (int)mc.m_91268_().m_85442_());
        RenderSystem.activeTexture((int)33984);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthFunc((int)515);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

