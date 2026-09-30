/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk
 *  net.minecraft.client.renderer.chunk.RenderRegionCache
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.SectionCaptureAccess;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ChunkRenderDispatcher.RenderChunk.class})
public abstract class ChunkRenderDispatcherRenderChunkMixin {
    @Shadow
    @Final
    private BlockPos.MutableBlockPos f_112793_;

    @Inject(method={"createCompileTask"}, at={@At(value="RETURN")}, require=0)
    private void luxium$attachSectionOrigin(RenderRegionCache renderRegionCache, CallbackInfoReturnable<Object> cir) {
        Object object = cir.getReturnValue();
        if (object instanceof SectionCaptureAccess) {
            SectionCaptureAccess access = (SectionCaptureAccess)object;
            access.luxium$setSectionOrigin(this.f_112793_.m_7949_());
        }
    }
}

