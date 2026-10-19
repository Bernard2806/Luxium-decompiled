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
        if (cir.getReturnValue() instanceof SectionCaptureAccess access) {
            access.luxium$setSectionOrigin(this.f_112793_.m_7949_());
        }
    }
}
