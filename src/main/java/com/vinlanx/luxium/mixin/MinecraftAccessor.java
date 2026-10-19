package com.vinlanx.luxium.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value={Minecraft.class})
public interface MinecraftAccessor {
    @Accessor(value="mainRenderTarget")
    public RenderTarget luxium$getMainRenderTargetField();
    @Mutable
    @Accessor(value="mainRenderTarget")
    public void luxium$setMainRenderTargetField(RenderTarget var1);
}
