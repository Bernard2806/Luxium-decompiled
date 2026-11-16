package com.vinlanx.luxium.Testing.ExplosionTesting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.vinlanx.luxium.platform.ShaderRegistrar;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ExplosionShader {
    private static final Logger
    LOGGER = LogManager.getLogger();
    @Nullable private static ShaderInstance particleShader;
    private ExplosionShader()
    { }

    public static void register(ShaderRegistrar event) {
        try {
            event.registerShader(new ResourceLocation("luxium", "explosion_particle"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, value -> particleShader = value ); }
 catch (IOException exception) {
            LOGGER.error("Failed to load explosion particle shader", exception);
        }
    }

    @Nullable static ShaderInstance get() {
 return particleShader;
    }
    }
