package com.vinlanx.luxium.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value={RenderTarget.class})
public interface RenderTargetAccessor {
    @Accessor(value="colorTextureId")
    public int luxium$getColorTextureIdRaw();
    @Accessor(value="colorTextureId")
    public void luxium$setColorTextureIdRaw(int var1);
}
