/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.client.Minecraft
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.coordinates.Vec3Argument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterClientCommandsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.Testing;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vinlanx.luxium.Testing.AirDistortion;
import com.vinlanx.luxium.Testing.BlackHole;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionWorld;
import com.vinlanx.luxium.Testing.Grass;
import com.vinlanx.luxium.Testing.ShockWaveTest;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.client.posteffects.skyvolumetrigodrays;
import com.vinlanx.luxium.client.reflections.RainPuddleManager;
import com.vinlanx.luxium.client.reflections.RainPuddleSurface;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import com.vinlanx.luxium.client.reflections.ReflectionMaterialRegistry;
import com.vinlanx.luxium.rtx.Coloredlight;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCommands {
    private ModCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder skylightTest = (LiteralArgumentBuilder)Commands.m_82127_((String)"skylighttest").executes(ModCommands::executeSkylightTest);
        LiteralArgumentBuilder flashlight = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"flashlight").then(Commands.m_82127_((String)"off").executes(ctx -> ModCommands.executeFlashlightOff((CommandContext<CommandSourceStack>)ctx)))).then(((LiteralArgumentBuilder)Commands.m_82127_((String)"on").executes(ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, 15, 180))).then(((RequiredArgumentBuilder)Commands.m_82129_((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)15)).executes(ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"power"), 180))).then(Commands.m_82129_((String)"angle", (ArgumentType)IntegerArgumentType.integer((int)1, (int)180)).executes(ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"power"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"angle"))))));
        LiteralArgumentBuilder grass = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"grass").then(Commands.m_82127_((String)"off").executes(ctx -> ModCommands.executeGrassOff((CommandContext<CommandSourceStack>)ctx)))).then(((LiteralArgumentBuilder)Commands.m_82127_((String)"on").executes(ctx -> ModCommands.executeGrassOn((CommandContext<CommandSourceStack>)ctx, Grass.getRenderRadius()))).then(Commands.m_82129_((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes(ctx -> ModCommands.executeGrassOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"radius")))));
        LiteralArgumentBuilder rtxColor = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"rtxcolor").then(Commands.m_82127_((String)"spectre").then(Commands.m_82129_((String)"speed", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes(ctx -> ModCommands.executeRtxColorSpectre((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"speed")))))).then(Commands.m_82127_((String)"police").executes(ctx -> ModCommands.executeRtxColorPolice((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82127_((String)"police2").executes(ctx -> ModCommands.executeRtxColorPolice2((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82127_((String)"off").executes(ctx -> ModCommands.executeRtxColorOff((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82127_((String)"reset").executes(ctx -> ModCommands.executeRtxColorReset((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82129_((String)"red", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).then(Commands.m_82129_((String)"green", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).then(Commands.m_82129_((String)"blue", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).executes(ctx -> ModCommands.executeRtxColorSet((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"red"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"green"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"blue"))))));
        LiteralArgumentBuilder rain = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"rain").executes(ctx -> ModCommands.executeRain((CommandContext<CommandSourceStack>)ctx, 1))).then(Commands.m_82129_((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes(ctx -> ModCommands.executeRain((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"size"))));
        LiteralArgumentBuilder blackHole = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"blackhole").executes(ctx -> ModCommands.executeBlackHole((CommandContext<CommandSourceStack>)ctx, 1))).then(Commands.m_82127_((String)"delete").executes(ctx -> ModCommands.executeBlackHoleDelete((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82129_((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes(ctx -> ModCommands.executeBlackHole((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"size"))));
        LiteralArgumentBuilder airDistortion = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"airdistortion").executes(ctx -> ModCommands.executeAirDistortion((CommandContext<CommandSourceStack>)ctx, 1))).then(Commands.m_82127_((String)"explosion").then(Commands.m_82129_((String)"position", (ArgumentType)Vec3Argument.m_120841_()).then(Commands.m_82129_((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)40)).then(Commands.m_82129_((String)"speed", (ArgumentType)IntegerArgumentType.integer((int)1, (int)300)).executes(ctx -> ModCommands.executeAirDistortionExplosion((CommandContext<CommandSourceStack>)ctx, Vec3Argument.m_120844_((CommandContext)ctx, (String)"position"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"power"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"speed")))))))).then(Commands.m_82127_((String)"delete").executes(ctx -> ModCommands.executeAirDistortionDelete((CommandContext<CommandSourceStack>)ctx)))).then(Commands.m_82129_((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes(ctx -> ModCommands.executeAirDistortion((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"radius"))));
        LiteralArgumentBuilder explosionXul = (LiteralArgumentBuilder)Commands.m_82127_((String)"explosionxul").then(Commands.m_82129_((String)"position", (ArgumentType)Vec3Argument.m_120841_()).then(Commands.m_82129_((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes(ctx -> ModCommands.executeExplosionXul((CommandContext<CommandSourceStack>)ctx, Vec3Argument.m_120844_((CommandContext)ctx, (String)"position"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"power")))));
        event.getDispatcher().register(skylightTest);
        event.getDispatcher().register(flashlight);
        event.getDispatcher().register(grass);
        event.getDispatcher().register(rtxColor);
        event.getDispatcher().register(rain);
        event.getDispatcher().register(blackHole);
        event.getDispatcher().register(airDistortion);
        event.getDispatcher().register(explosionXul);
    }

    private static int executeSkylightTest(CommandContext<CommandSourceStack> ctx) {
        int skyLight = skyvolumetrigodrays.sampleCameraSkylight();
        if (skyLight < 0) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Skylight test requires an active client world"));
            return 0;
        }
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Volumetric effective camera light: " + skyLight + "/15")), false);
        return 1;
    }

    private static int executeFlashlightOn(CommandContext<CommandSourceStack> ctx, int power, int angle) {
        TestFlashLight.enable(power, angle);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Flashlight enabled: power=" + TestFlashLight.getPower() + ", angle=" + TestFlashLight.getConeAngle() + ", rays=" + TestFlashLight.getCurrentRayCount())), false);
        return 1;
    }

    private static int executeFlashlightOff(CommandContext<CommandSourceStack> ctx) {
        TestFlashLight.disable();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"Flashlight disabled"), false);
        return 1;
    }

    private static int executeGrassOn(CommandContext<CommandSourceStack> ctx, int radius) {
        Grass.enable();
        Grass.setRenderRadius(radius);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Grass rendering enabled (radius=" + Grass.getRenderRadius() + " blocks)")), false);
        return 1;
    }

    private static int executeGrassOff(CommandContext<CommandSourceStack> ctx) {
        Grass.disable();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"Grass rendering disabled"), false);
        return 1;
    }

    private static int executeRtxColorSet(CommandContext<CommandSourceStack> ctx, int red, int green, int blue) {
        Coloredlight.setColor(red, green, blue);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("RTX block light color set to RGB(" + Coloredlight.getRed() + ", " + Coloredlight.getGreen() + ", " + Coloredlight.getBlue() + ")")), false);
        return 1;
    }

    private static int executeRtxColorReset(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.reset();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"RTX block light color reset to vanilla"), false);
        return 1;
    }

    private static int executeRtxColorSpectre(CommandContext<CommandSourceStack> ctx, int speed) {
        Coloredlight.enableSpectre(speed);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("\u00a7aRTX Color Spectre enabled with speed: \u00a7e" + speed)), false);
        return 1;
    }

    private static int executeRtxColorOff(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.disableSpectre();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"\u00a7cRTX Color disabled"), false);
        return 1;
    }

    private static int executeRtxColorPolice(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.enablePolice();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"\u00a79\u00a7lPOLICE MODE ACTIVATED \u00a7c\u00a7l[WEE WOO WEE WOO]"), false);
        return 1;
    }

    private static int executeRtxColorPolice2(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.enablePolice2();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"\u00a79\u00a7lPOLICE MODE 2 ACTIVATED \u00a7c\u00a7l[FLASH FLASH FLASH]"), false);
        return 1;
    }

    private static int executeRain(CommandContext<CommandSourceStack> ctx, int size) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Rain puddles require an active client world"));
            return 0;
        }
        BlockPos targetPos = BlockPos.m_274561_((double)mc.f_91074_.m_20185_(), (double)(mc.f_91074_.m_20191_().f_82289_ - 0.05), (double)mc.f_91074_.m_20189_());
        BlockState state = mc.f_91073_.m_8055_(targetPos);
        ReflectionMaterial material = ReflectionMaterialRegistry.find(state);
        if (material == null || !material.shouldReflectFace(mc, targetPos, state, Direction.UP)) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Block under player is not a supported reflective top face"));
            return 0;
        }
        RainPuddleSurface surface = RainPuddleManager.get().placeRandomPuddle(mc, targetPos, size);
        if (surface == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"No rain heightmaps found or puddle footprint has no reflective neighbors"));
            return 0;
        }
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Rain puddle placed: map=" + surface.getHeightmap().getDisplayName() + ", size=" + surface.getSpanBlocks() + ", reflective blocks=" + surface.getCoveredBlockCount())), false);
        return 1;
    }

    private static int executeBlackHole(CommandContext<CommandSourceStack> ctx, int radius) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Player is not available"));
            return 0;
        }
        BlackHole.spawnAtPlayer(radius);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Black hole spawned at your position (radius=" + radius + ")")), false);
        return 1;
    }

    private static int executeBlackHoleDelete(CommandContext<CommandSourceStack> ctx) {
        BlackHole.clear();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"All black holes removed"), false);
        return 1;
    }

    private static int executeAirDistortion(CommandContext<CommandSourceStack> ctx, int radius) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Player is not available"));
            return 0;
        }
        AirDistortion.spawnAtPlayer(radius);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Air distortion spawned at your position (radius=" + radius + ")")), false);
        return 1;
    }

    private static int executeAirDistortionDelete(CommandContext<CommandSourceStack> ctx) {
        AirDistortion.clear();
        ShockWaveTest.clear();
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)"All air distortions removed"), false);
        return 1;
    }

    private static int executeAirDistortionExplosion(CommandContext<CommandSourceStack> ctx, Vec3 position, int power, int speed) {
        ShockWaveTest.spawnExplosion(position, power, speed);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Air distortion shockwave spawned at " + ModCommands.formatCoordinate(position.f_82479_) + " " + ModCommands.formatCoordinate(position.f_82480_) + " " + ModCommands.formatCoordinate(position.f_82481_) + " (power=" + power + ", speed=" + speed + " blocks/s)")), false);
        return 1;
    }

    private static int executeExplosionXul(CommandContext<CommandSourceStack> ctx, Vec3 position, int power) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_((String)"Explosion simulation requires an active client world"));
            return 0;
        }
        ExplosionWorld.get().trigger(position, power);
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Explosion XUL spawned at " + ModCommands.formatCoordinate(position.f_82479_) + " " + ModCommands.formatCoordinate(position.f_82480_) + " " + ModCommands.formatCoordinate(position.f_82481_) + " (power=" + power + ")")), false);
        return 1;
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}

