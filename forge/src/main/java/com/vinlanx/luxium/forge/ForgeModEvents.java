package com.vinlanx.luxium.forge;

import com.vinlanx.luxium.Testing.LuxiumMasterKeyMapping;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionShader;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.f3.LuxiumDebugHud;
import com.vinlanx.luxium.platform.ShaderRegistrar;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeModEvents {
    private ForgeModEvents()
    { }

    @SubscribeEvent public static void shaders(RegisterShadersEvent event) {
        ShaderRegistrar
        registrar = (id, format, loaded) ->
 event.registerShader(new ShaderInstance(event.getResourceProvider(), id, format), loaded);
        ShaderManager.onRegisterShaders(registrar);
        ExplosionShader.register(registrar);
    }

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {

        event.register(LuxiumMasterKeyMapping.TOGGLE);
    }

    @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {

        event.registerAboveAll(
            "debug_hud", (gui, graphics, partialTick, width, height) -> LuxiumDebugHud.render(gui.getMinecraft(), graphics, partialTick, width, height) );
 }
        }
