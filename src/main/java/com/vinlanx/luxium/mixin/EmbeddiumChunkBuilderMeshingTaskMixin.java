/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ReportedException
 *  net.minecraft.core.BlockPos
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.mixin.EmbeddiumChunkBuilderMeshingTaskAccessor;
import com.vinlanx.luxium.mixin.EmbeddiumRenderSectionAccessor;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets={"org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask"})
public abstract class EmbeddiumChunkBuilderMeshingTaskMixin {
    @Inject(method={"execute"}, at={@At(value="HEAD")}, remap=false, require=0)
    private void luxium$beginEmbeddiumSectionCapture(CallbackInfoReturnable<Object> cir) {
        Object renderSection = ((EmbeddiumChunkBuilderMeshingTaskAccessor)((Object)this)).luxium$getRender();
        if (renderSection instanceof EmbeddiumRenderSectionAccessor) {
            EmbeddiumRenderSectionAccessor access = (EmbeddiumRenderSectionAccessor)renderSection;
            ExposedFaceService.get().beginSectionCapture(new BlockPos(access.luxium$getOriginX(), access.luxium$getOriginY(), access.luxium$getOriginZ()));
            return;
        }
        ExposedFaceService.get().discardSectionCapture();
    }

    @Inject(method={"execute"}, at={@At(value="RETURN")}, remap=false, require=0)
    private void luxium$finishEmbeddiumSectionCapture(CallbackInfoReturnable<Object> cir) {
        ExposedFaceService.get().finishSectionCapture();
    }

    @Inject(method={"fillCrashInfo"}, at={@At(value="HEAD")}, remap=false, require=0)
    private void luxium$discardEmbeddiumCaptureOnCrash(CallbackInfoReturnable<ReportedException> cir) {
        ExposedFaceService.get().discardSectionCapture();
    }
}

