package com.vinlanx.luxium.platform;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface ShaderRegistrar {
    void registerShader(ResourceLocation var1, VertexFormat var2, Consumer<ShaderInstance> var3) throws IOException;
}
