/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.ChunkBufferBuilderPack
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import com.vinlanx.luxium.rtx.soasnottointerfere.SectionCaptureAccess;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask"})
public abstract class ChunkRenderDispatcherRebuildTaskMixin
implements SectionCaptureAccess {
    @Unique
    @Nullable
    private BlockPos luxium$sectionOrigin;

    @Override
    public void luxium$setSectionOrigin(@Nullable BlockPos origin) {
        this.luxium$sectionOrigin = origin;
    }

    @Override
    @Nullable
    public BlockPos luxium$getSectionOrigin() {
        return this.luxium$sectionOrigin;
    }

    @Inject(method={"compile"}, at={@At(value="HEAD")}, require=0)
    private void luxium$beginSectionCapture(float cameraX, float cameraY, float cameraZ, ChunkBufferBuilderPack buffers, CallbackInfoReturnable<Object> cir) {
        ExposedFaceService.get().beginSectionCapture(this.luxium$sectionOrigin);
    }

    @Inject(method={"compile"}, at={@At(value="RETURN")}, require=0)
    private void luxium$finishSectionCapture(float cameraX, float cameraY, float cameraZ, ChunkBufferBuilderPack buffers, CallbackInfoReturnable<Object> cir) {
        ExposedFaceService.get().finishSectionCapture();
    }
}

