package com.vinlanx.luxium.Testing; import com.mojang.brigadier.arguments.ArgumentType; import com.mojang.brigadier.arguments.IntegerArgumentType; import com.mojang.brigadier.builder.LiteralArgumentBuilder; import com.mojang.brigadier.builder.RequiredArgumentBuilder; import com.mojang.brigadier.context.CommandContext; import com.vinlanx.luxium.Testing.AirDistortion;
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

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
 public final class ModCommands { private ModCommands() { }


    @SubscribeEvent public static void onRegisterClientCommands(RegisterClientCommandsEvent event) { LiteralArgumentBuilder<CommandSourceStack>

skylightTest = (LiteralArgumentBuilder)Commands.literal
                ((String)"skylighttest").executes
                (ModCommands::executeSkylightTest);

        LiteralArgumentBuilder<CommandSourceStack> flashlight = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"flashlight").then
                (Commands.literal((String)"off").executes
                (ctx -> ModCommands.executeFlashlightOff((CommandContext<CommandSourceStack>)ctx)))).then
                (((LiteralArgumentBuilder)Commands.literal((String)"on").executes
                (ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, 15, 180))).then
                (((RequiredArgumentBuilder)Commands.argument((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)15)).executes
                (ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"power"), 180))).then

                (Commands.argument((String)"angle", (ArgumentType)IntegerArgumentType.integer((int)1, (int)180)).executes
                (ctx -> ModCommands.executeFlashlightOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"power"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"angle"))))));

        LiteralArgumentBuilder<CommandSourceStack> grass = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"grass").then
                (Commands.literal((String)"off").executes
                (ctx -> ModCommands.executeGrassOff((CommandContext<CommandSourceStack>)ctx)))).then
                (((LiteralArgumentBuilder)Commands.literal((String)"on").executes

                (ctx -> ModCommands.executeGrassOn((CommandContext<CommandSourceStack>)ctx, Grass.getRenderRadius()))).then

                (Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes
                (ctx -> ModCommands.executeGrassOn((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"radius")))));

        LiteralArgumentBuilder<CommandSourceStack> rtxColor = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"rtxcolor").then
                (Commands.literal((String)"spectre").then
                (Commands.argument((String)"speed", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes
                (ctx -> ModCommands.executeRtxColorSpectre((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"speed")))))).then
                (Commands.literal((String)"police").executes
                (ctx -> ModCommands.executeRtxColorPolice((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.literal((String)"police2").executes
                (ctx -> ModCommands.executeRtxColorPolice2((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.literal((String)"off").executes
                (ctx -> ModCommands.executeRtxColorOff((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.literal((String)"reset").executes
                (ctx -> ModCommands.executeRtxColorReset((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.argument((String)"red", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).then
                (Commands.argument((String)"green", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).then
                (Commands.argument((String)"blue", (ArgumentType)IntegerArgumentType.integer((int)0, (int)255)).executes
                (ctx -> ModCommands.executeRtxColorSet((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"red"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"green"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"blue"))))));

        LiteralArgumentBuilder<CommandSourceStack> rain = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"rain").executes
                (ctx -> ModCommands.executeRain((CommandContext<CommandSourceStack>)ctx, 1))).then
                (Commands.argument((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes
                (ctx -> ModCommands.executeRain((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"size"))));

        LiteralArgumentBuilder<CommandSourceStack> blackHole = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"blackhole").executes
                (ctx -> ModCommands.executeBlackHole((CommandContext<CommandSourceStack>)ctx, 1))).then
                (Commands.literal((String)"delete").executes
                (ctx -> ModCommands.executeBlackHoleDelete((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.argument((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes
                (ctx -> ModCommands.executeBlackHole((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"size"))));

        LiteralArgumentBuilder<CommandSourceStack> airDistortion = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"airdistortion").executes
                (ctx -> ModCommands.executeAirDistortion((CommandContext<CommandSourceStack>)ctx, 1))).then
                (Commands.literal((String)"explosion").then
                (Commands.argument((String)"position", (ArgumentType)Vec3Argument.vec3()).then
                (Commands.argument((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)40)).then
                (Commands.argument((String)"speed", (ArgumentType)IntegerArgumentType.integer((int)1, (int)300)).executes
                (ctx -> ModCommands.executeAirDistortionExplosion((CommandContext<CommandSourceStack>)ctx, Vec3Argument.getVec3

                ((CommandContext)ctx, (String)"position"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"power"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"speed")))))))).then
                (Commands.literal((String)"delete").executes
                (ctx -> ModCommands.executeAirDistortionDelete((CommandContext<CommandSourceStack>)ctx)))).then
                (Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes
                (ctx -> ModCommands.executeAirDistortion((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger

                ((CommandContext)ctx, (String)"radius"))));

        LiteralArgumentBuilder<CommandSourceStack> explosionXul = (LiteralArgumentBuilder)Commands.literal
                ((String)"explosionxul").then
                (Commands.argument((String)"position", (ArgumentType)Vec3Argument.vec3()).then
                (Commands.argument((String)"power", (ArgumentType)IntegerArgumentType.integer((int)1, (int)100)).executes
                (ctx -> ModCommands.executeExplosionXul((CommandContext<CommandSourceStack>)ctx, Vec3Argument.getVec3

                ((CommandContext)ctx, (String)"position"), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"power")))));

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
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Skylight test requires an active client world"));
            return 0;
        }

        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Volumetric effective camera light: " + skyLight + "/15")), false);


        return 1;
    }


    private static int executeFlashlightOn(CommandContext<CommandSourceStack> ctx, int power, int angle) {
        TestFlashLight.enable(power, angle);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Flashlight enabled: power=" + TestFlashLight.getPower
                () + ", angle=" + TestFlashLight.getConeAngle
                () + ", rays=" + TestFlashLight.getCurrentRayCount
                ())), false);

        return 1;
    }

    private static int executeFlashlightOff(CommandContext<CommandSourceStack> ctx) {
        TestFlashLight.disable();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"Flashlight disabled"), false);
        return 1;
    }

    private static int executeGrassOn(CommandContext<CommandSourceStack> ctx, int radius) {
        Grass.enable();
        Grass.setRenderRadius(radius);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Grass rendering enabled (radius=" + Grass.getRenderRadius
                () + " blocks)")), false);

        return 1;
    }

    private static int executeGrassOff(CommandContext<CommandSourceStack> ctx) {
        Grass.disable();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"Grass rendering disabled"), false);
        return 1;
    }

    private static int executeRtxColorSet(CommandContext<CommandSourceStack> ctx, int red, int green, int blue) {
        Coloredlight.setColor(red, green, blue);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("RTX block light color set to RGB(" + Coloredlight.getRed

                () + ", " + Coloredlight.getGreen
                () + ", " + Coloredlight.getBlue
                () + ")")), false);

        return 1;
    }

    private static int executeRtxColorReset(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.reset();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"RTX block light color reset to vanilla"), false);
        return 1;
    }

    private static int executeRtxColorSpectre(CommandContext<CommandSourceStack> ctx, int speed) {
        Coloredlight.enableSpectre(speed);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aRTX Color Spectre enabled with speed: \u00a7e" + speed)), false);


        return 1;
    }

    private static int executeRtxColorOff(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.disableSpectre();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"\u00a7cRTX Color disabled"), false);
        return 1;
    }

    private static int executeRtxColorPolice(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.enablePolice();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"\u00a79\u00a7lPOLICE MODE ACTIVATED \u00a7c\u00a7l[WEE WOO WEE WOO]"), false);


        return 1;
    }

    private static int executeRtxColorPolice2(CommandContext<CommandSourceStack> ctx) {
        Coloredlight.enablePolice2();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"\u00a79\u00a7lPOLICE MODE 2 ACTIVATED \u00a7c\u00a7l[FLASH FLASH FLASH]"), false);


        return 1;
    }

    private static int executeRain(CommandContext<CommandSourceStack> ctx, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Rain puddles require an active client world"));
            return 0;
        }

        BlockPos targetPos = BlockPos.containing((double)mc.player.getX
                (), (double)(mc.player.getBoundingBox
                ().minY - 0.05), (double)mc.player.getZ
                ());

        BlockState state = mc.level.getBlockState(targetPos);
        ReflectionMaterial material = ReflectionMaterialRegistry.find(state);
        if (material == null || !material.shouldReflectFace(mc, targetPos, state, Direction.UP)) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Block under player is not a supported reflective top face"));


            return 0;
        }

        RainPuddleSurface surface = RainPuddleManager.get().placeRandomPuddle(mc, targetPos, size);
        if (surface == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"No rain heightmaps found or puddle footprint has no reflective neighbors"));


            return 0;
        }

        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Rain puddle placed: map=" + surface.getHeightmap
                ().getDisplayName() + ", size=" + surface.getSpanBlocks
                () + ", reflective blocks=" + surface.getCoveredBlockCount
                ())), false);

        return 1;
    }

    private static int executeBlackHole(CommandContext<CommandSourceStack> ctx, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Player is not available"));
            return 0;
        }

        BlackHole.spawnAtPlayer(radius);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Black hole spawned at your position (radius=" + radius + ")")), false);


        return 1;
    }

    private static int executeBlackHoleDelete(CommandContext<CommandSourceStack> ctx) {
        BlackHole.clear();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"All black holes removed"), false);
        return 1;
    }

    private static int executeAirDistortion(CommandContext<CommandSourceStack> ctx, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Player is not available"));
            return 0;
        }

        AirDistortion.spawnAtPlayer(radius);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Air distortion spawned at your position (radius=" + radius + ")")), false);


        return 1;
    }

    private static int executeAirDistortionDelete(CommandContext<CommandSourceStack> ctx) {
        AirDistortion.clear();
        ShockWaveTest.clear();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"All air distortions removed"), false);
        return 1;
    }

    private static int executeAirDistortionExplosion(CommandContext<CommandSourceStack> ctx, Vec3 position, int power, int speed) {
        ShockWaveTest.spawnExplosion(position, power, speed);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Air distortion shockwave spawned at " + ModCommands.formatCoordinate

                (position.x) + " " + ModCommands.formatCoordinate
                (position.y) + " " + ModCommands.formatCoordinate
                (position.z) + " (power=" + power + ", speed=" + speed + " blocks/s)")), false);


        return 1;
    }

    private static int executeExplosionXul(CommandContext<CommandSourceStack> ctx, Vec3 position, int power) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Explosion simulation requires an active client world"));
            return 0;
        }
        ExplosionWorld.get().trigger(position, power);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Explosion XUL spawned at " + ModCommands.formatCoordinate

                (position.x) + " " + ModCommands.formatCoordinate
                (position.y) + " " + ModCommands.formatCoordinate
                (position.z) + " (power=" + power + ")")), false);


        return 1;
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
    }
