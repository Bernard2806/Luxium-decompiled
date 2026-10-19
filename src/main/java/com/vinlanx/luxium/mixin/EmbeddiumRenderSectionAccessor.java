package com.vinlanx.luxium.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
@Pseudo
@Mixin(targets={"org.embeddedt.embeddium.impl.render.chunk.RenderSection"})
public interface EmbeddiumRenderSectionAccessor {
    @Invoker(value="getOriginX")
    public int luxium$getOriginX();
    @Invoker(value="getOriginY")
    public int luxium$getOriginY();
    @Invoker(value="getOriginZ")
    public int luxium$getOriginZ();
}
