package com.vinlanx.luxium.mixin;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value={GameRenderer.class})
public interface GameRendererAccessor {
    @Invoker(value="reloadShaders")
    public void luxium$reloadShaders(ResourceProvider var1);
}
