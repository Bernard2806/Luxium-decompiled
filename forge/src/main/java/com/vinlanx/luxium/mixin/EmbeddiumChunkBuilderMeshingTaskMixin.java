package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = {ChunkBuilderMeshingTask.class}, remap = false ) public abstract

 class EmbeddiumChunkBuilderMeshingTaskMixin {
    @Inject(
        method = {"execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at = {@At("HEAD")}, remap = false, require = 1 ) private void luxium$beginEmbeddiumSectionCapture(CallbackInfoReturnable<ChunkBuildOutput> cir) { RenderSection

 section = ((EmbeddiumChunkBuilderMeshingTaskAccessor)(Object)this).luxium$getRender();
        ExposedFaceService.get().beginSectionCapture(new BlockPos(section.getOriginX
(), section.getOriginY(), section.getOriginZ()));
    }

    @Inject(method = {"execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at = {@At("RETURN")}, remap = false, require = 1 ) private void luxium$finishEmbeddiumSectionCapture(CallbackInfoReturnable<ChunkBuildOutput> cir) {

 if (cir.getReturnValue() == null) { ExposedFaceService.get().discardSectionCapture();
        } else { ExposedFaceService.get().finishSectionCapture();
        } }

    @Inject(method = {"fillCrashInfo"}, at = {@At("HEAD")}, remap = false, require = 1 ) private void luxium$discardEmbeddiumCaptureOnCrash(CallbackInfoReturnable<ReportedException> cir) { ExposedFaceService.get

 ().discardSectionCapture();
    }
    }
