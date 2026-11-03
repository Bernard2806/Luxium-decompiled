package com.vinlanx.luxium.mixin;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value={LightTexture.class})
public interface LightTextureAccessor {
    @Accessor(value="updateLightTexture")
    public void luxium$setUpdateLightTexture(boolean var1);
}
