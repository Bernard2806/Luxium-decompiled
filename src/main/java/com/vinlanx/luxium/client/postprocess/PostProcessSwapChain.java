/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vinlanx.luxium.mixin.RenderTargetAccessor;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;

public final class PostProcessSwapChain {
    private static TextureTarget scratch;

    private PostProcessSwapChain() {
    }

    public static RenderTarget begin(RenderTarget main) {
        PostProcessSwapChain.ensure(main);
        if (PostProcessSwapChain.scratch.f_83922_ != main.f_83922_) {
            scratch.m_83936_(main.f_83922_);
        }
        scratch.m_83947_(true);
        return scratch;
    }

    public static void commit(RenderTarget main) {
        if (scratch == null || PostProcessSwapChain.scratch.f_83915_ != main.f_83915_ || PostProcessSwapChain.scratch.f_83916_ != main.f_83916_) {
            main.m_83947_(true);
            return;
        }
        RenderTargetAccessor mainAccess = (RenderTargetAccessor)main;
        RenderTargetAccessor scratchAccess = (RenderTargetAccessor)scratch;
        int oldMain = mainAccess.luxium$getColorTextureIdRaw();
        int newMain = scratchAccess.luxium$getColorTextureIdRaw();
        if (oldMain <= 0 || newMain <= 0 || oldMain == newMain) {
            main.m_83947_(true);
            return;
        }
        mainAccess.luxium$setColorTextureIdRaw(newMain);
        scratchAccess.luxium$setColorTextureIdRaw(oldMain);
        PostProcessSwapChain.attachColor(main.f_83920_, newMain);
        PostProcessSwapChain.attachColor(PostProcessSwapChain.scratch.f_83920_, oldMain);
        main.m_83947_(true);
    }

    public static int scratchTextureId() {
        return scratch != null ? scratch.m_83975_() : -1;
    }

    private static void ensure(RenderTarget main) {
        if (scratch == null) {
            scratch = new TextureTarget(main.f_83915_, main.f_83916_, false, Minecraft.f_91002_);
            scratch.m_83936_(9728);
        } else if (PostProcessSwapChain.scratch.f_83915_ != main.f_83915_ || PostProcessSwapChain.scratch.f_83916_ != main.f_83916_) {
            scratch.m_83941_(main.f_83915_, main.f_83916_, Minecraft.f_91002_);
            scratch.m_83936_(9728);
        }
    }

    private static void attachColor(int framebuffer, int texture) {
        GlStateManager._glBindFramebuffer((int)36160, (int)framebuffer);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)texture, (int)0);
    }
}

