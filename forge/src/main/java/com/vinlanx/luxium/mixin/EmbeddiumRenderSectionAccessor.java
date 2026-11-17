package com.vinlanx.luxium.mixin;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value={RenderSection.class}, remap=false) public interface EmbeddiumRenderSectionAccessor {
    @Invoker(value="getOriginX") public int luxium$getOriginX();
    @Invoker(value="getOriginY") public int luxium$getOriginY();
    @Invoker(value="getOriginZ") public int luxium$getOriginZ();
}
