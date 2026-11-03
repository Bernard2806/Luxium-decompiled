package com.vinlanx.luxium.forge;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.Testing.ModCommands;
import com.vinlanx.luxium.client.ConfigScreen.LuxiumConfigScreen;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleCommands;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("luxium") public final class ForgeLuxium { public ForgeLuxium()
 {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ForgeLuxium.Client::initialize);
    }

    private static final class Client {
        private static void initialize() {
            LuxiumPlatform.initialize(new ForgePlatform());
            Config.register();
            ModLoadingContext.get() .registerExtensionPoint(ConfigScreenFactory.class, () ->
new ConfigScreenFactory((mc, parent) -> new LuxiumConfigScreen(parent)));
            MinecraftForge.EVENT_BUS.register(ForgeClientEvents.class);
            MinecraftForge.EVENT_BUS.register(ModCommands.class);
            MinecraftForge.EVENT_BUS.register(RainPuddleCommands.class);
            FMLJavaModLoadingContext.get().getModEventBus().register(ForgeModEvents.class);
        }
    }
    }
