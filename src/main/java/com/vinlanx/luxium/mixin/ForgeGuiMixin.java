/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"net.minecraftforge.client.gui.overlay.ForgeGui"})
public abstract class ForgeGuiMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")}, require=0)
    private void luxium$resetForgeGuiState(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState();
    }

    @Inject(method={"setupOverlayRenderState"}, at={@At(value="HEAD")}, require=0)
    private void luxium$restoreTargetBeforeOverlay(boolean blend, boolean depthTest, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState();
        Minecraft mc = Minecraft.m_91087_();
        RenderSystem.viewport((int)0, (int)0, (int)mc.m_91268_().m_85441_(), (int)mc.m_91268_().m_85442_());
        RenderSystem.activeTexture((int)33984);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

