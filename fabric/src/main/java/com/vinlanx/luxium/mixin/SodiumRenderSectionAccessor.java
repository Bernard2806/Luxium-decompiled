package com.vinlanx.luxium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
    value = {RenderSection.class},
    remap = false
)
public interface SodiumRenderSectionAccessor {
    @Invoker(
        value = "getOriginX",
        remap = false
    )
    int luxium$getOriginX();

    @Invoker(
        value = "getOriginY",
        remap = false
    )
    int luxium$getOriginY();

    @Invoker(
        value = "getOriginZ",
        remap = false
    )
    int luxium$getOriginZ();
}
