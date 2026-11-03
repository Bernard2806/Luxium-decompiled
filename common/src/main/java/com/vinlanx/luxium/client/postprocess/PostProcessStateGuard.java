package com.vinlanx.luxium.client.postprocess;






import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;


import org.jetbrains.annotations.Nullable;

public final class PostProcessStateGuard {
    @Nullable private static RenderTarget canonicalMainTarget; private static boolean repairedGuiCompositeStateThisFrame; private static boolean unsafeGuiCompositeStateSeenThisSession; private PostProcessStateGuard()
 {
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
            target.bindWrite(true);
        } else if (mc.getMainRenderTarget() != null) {
            mc.getMainRenderTarget().bindWrite(true);
            repairedTarget = !(current instanceof MainTarget);
        }

        repairedGuiCompositeStateThisFrame |= repairedTarget;
        unsafeGuiCompositeStateSeenThisSession |= repairedTarget;
    }

    public static void restoreGuiCompositeState() {
        PostProcessStateGuard.restoreGuiCompositeState(Minecraft.getInstance());
    }

    public static void restoreGuiCompositeState(Minecraft mc) {
        PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);

        RenderTarget target = mc.getMainRenderTarget();
        int viewportWidth = mc.getWindow().getWidth();
        int viewportHeight = mc.getWindow().getHeight();
        if (target != null) {
            viewportWidth = target.viewWidth;
            viewportHeight = target.viewHeight;
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
