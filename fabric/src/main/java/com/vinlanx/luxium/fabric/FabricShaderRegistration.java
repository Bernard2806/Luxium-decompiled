package com.vinlanx.luxium.fabric;

import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionShader;
import com.vinlanx.luxium.client.ShaderManager;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.RegistrationContext;

public final class FabricShaderRegistration {
    private static boolean registered;

    private FabricShaderRegistration() { }

    public static void beginReload() {
        registered = false;
    }

    public static void register(RegistrationContext context) {
        if (!(!registered)) { return;
 } registered = true;
            ShaderManager.onRegisterShaders(context::register);
            ExplosionShader.register(context::register);
    }
    }
