package com.vinlanx.luxium.mixin.sky;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value={SodiumWorldRenderer.class}, remap=false)
public interface SodiumWorldRendererAccessor {
    @Accessor(value="renderSectionManager")
    public RenderSectionManager luxium$getRenderSectionManager();
}
