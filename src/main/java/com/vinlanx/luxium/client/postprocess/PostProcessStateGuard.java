/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.MainTarget
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.postprocess;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class PostProcessStateGuard {
    @Nullable
    private static RenderTarget canonicalMainTarget;
    private static boolean repairedGuiCompositeStateThisFrame;
    private static boolean unsafeGuiCompositeStateSeenThisSession;

    private PostProcessStateGuard() {
    }

    public static void captureCanonicalMainTarget(Minecraft mc) {
        repairedGuiCompositeStateThisFrame = false;
        RenderTarget current = ((MinecraftAccessor)mc).luxium$getMainRenderTargetField();
        if (current instanceof MainTarget) {
            canonicalMainTarget = current;
        }
    }

    public static boolean repairedGuiCompositeStateThisFrame() {
        return repairedGuiCompositeStateThisFrame;
    }

    public static boolean unsafeGuiCompositeStateSeenThisSession() {
        return unsafeGuiCompositeStateSeenThisSession;
    }

    public static void restoreCanonicalMainTargetBinding(Minecraft mc) {
        if (TemporalFrameSystem.isWorldPassActive()) {
            TemporalFrameSystem.rebindWorldTarget();
            return;
        }
        if (LsrSystem.isWorldPassActive()) {
            LsrSystem.rebindWorldTarget();
            return;
        }
        RenderTarget target = canonicalMainTarget;
        MinecraftAccessor accessor = (MinecraftAccessor)mc;
        RenderTarget current = accessor.luxium$getMainRenderTargetField();
        boolean repairedTarget = false;
        if (target != null) {
            if (current != target) {
                accessor.luxium$setMainRenderTargetField(target);
                repairedTarget = true;
            }
            target.m_83947_(true);
        } else if (mc.m_91385_() != null) {
            mc.m_91385_().m_83947_(true);
            repairedTarget = !(current instanceof MainTarget);
        }
        repairedGuiCompositeStateThisFrame |= repairedTarget;
        unsafeGuiCompositeStateSeenThisSession |= repairedTarget;
    }

    public static void restoreGuiCompositeState() {
        PostProcessStateGuard.restoreGuiCompositeState(Minecraft.m_91087_());
    }

    public static void restoreGuiCompositeState(Minecraft mc) {
        PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);
        RenderTarget target = mc.m_91385_();
        int viewportWidth = mc.m_91268_().m_85441_();
        int viewportHeight = mc.m_91268_().m_85442_();
        if (target != null) {
            viewportWidth = target.f_83917_;
            viewportHeight = target.f_83918_;
        }
        RenderSystem.viewport((int)0, (int)0, (int)viewportWidth, (int)viewportHeight);
        RenderSystem.activeTexture((int)33984);
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.setShaderTexture((int)1, (int)0);
        RenderSystem.setShaderTexture((int)2, (int)0);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        GlStateManager._disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthFunc((int)515);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

