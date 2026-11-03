package com.vinlanx.luxium.fabric;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.Testing.LuxiumMasterKeyMapping;
import com.vinlanx.luxium.Testing.ModCommands;
import com.vinlanx.luxium.client.f3.LuxiumDebugHud;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleCommands;
import com.vinlanx.luxium.platform.ClientHooks;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft; public final class FabricLuxium implements ClientModInitializer {
    @Override public void onInitializeClient() { FabricPlatform
 platform = new FabricPlatform();
        LuxiumPlatform.initialize(platform);
        Config.register();
        KeyBindingHelper.registerKeyBinding(LuxiumMasterKeyMapping.TOGGLE);
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> ClientHooks.tick());
        CoreShaderRegistrationCallback.EVENT.register(FabricShaderRegistration::register);
        ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {
            ModCommands.onRegisterClientCommands(dispatcher);
            RainPuddleCommands.register(dispatcher);
        });
        HudRenderCallback.EVENT.register((HudRenderCallback)(graphics, partialTick) -> {
            Minecraft client = Minecraft.getInstance();
            LuxiumDebugHud.render(client, graphics, partialTick, client.getWindow
 ().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> platform.close());
    }
    }
