package com.vinlanx.luxium.Testing;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionWorld;
import com.vinlanx.luxium.client.posteffects.skyvolumetrigodrays;
import com.vinlanx.luxium.client.reflections.RainPuddleManager;
import com.vinlanx.luxium.client.reflections.RainPuddleSurface;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import com.vinlanx.luxium.client.reflections.ReflectionMaterialRegistry;
import com.vinlanx.luxium.rtx.Coloredlight;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3; public final class ModCommands { private ModCommands() { }

    public static void onRegisterClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource>
skylightTest = (LiteralArgumentBuilder<FabricClientCommandSource>)ClientCommandManager.literal
("skylighttest" ) .executes
(ModCommands::executeSkylightTest);
        LiteralArgumentBuilder<FabricClientCommandSource>
flashlight = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)ClientCommandManager.literal
("flashlight" ) .then
(ClientCommandManager.literal("off").executes
(ctx -> executeFlashlightOff(ctx))))
            .then(((LiteralArgumentBuilder)ClientCommandManager.literal("on").executes
(ctx -> executeFlashlightOn(ctx, 15, 180)))
                    .then(((RequiredArgumentBuilder)ClientCommandManager.argument("power", IntegerArgumentType.integer(1, 15))
                                .executes(ctx -> executeFlashlightOn(ctx, IntegerArgumentType.getInteger

 (ctx, "power"), 180)))
                            .then
                            (ClientCommandManager.argument("angle", IntegerArgumentType.integer(1, 180))
                                    .executes(ctx -> executeFlashlightOn(
                                                ctx, IntegerArgumentType.getInteger
 (ctx, "power"), IntegerArgumentType.getInteger
 (ctx, "angle") ) ) ) ) ); LiteralArgumentBuilder<FabricClientCommandSource>

grass = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)ClientCommandManager.literal
("grass" ) .then
(ClientCommandManager.literal("off").executes
(ctx -> executeGrassOff(ctx)))) .then
(((LiteralArgumentBuilder)ClientCommandManager.literal("on").executes

(ctx -> executeGrassOn(ctx, Grass.getRenderRadius())))
                    .then
                    (ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> executeGrassOn(ctx, IntegerArgumentType.getInteger

 (ctx, "radius"))) ) ); LiteralArgumentBuilder<FabricClientCommandSource>

rtxColor = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal
("rtxcolor" ) .then
(ClientCommandManager.literal("spectre")
                                        .then(ClientCommandManager.argument("speed", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> executeRtxColorSpectre(ctx, IntegerArgumentType.getInteger

 (ctx, "speed"))) ) )) .then
 (ClientCommandManager.literal("police").executes
(ctx -> executeRtxColorPolice(ctx))))
                        .then(ClientCommandManager.literal("police2").executes
(ctx -> executeRtxColorPolice2(ctx))))
                    .then(ClientCommandManager.literal("off").executes
(ctx -> executeRtxColorOff(ctx))))
                .then(ClientCommandManager.literal("reset").executes
(ctx -> executeRtxColorReset(ctx))))
            .then(ClientCommandManager.argument("red", IntegerArgumentType.integer(0, 255))
                    .then(ClientCommandManager.argument("green", IntegerArgumentType.integer(0, 255))
                            .then(ClientCommandManager.argument("blue", IntegerArgumentType.integer(0, 255))
                                    .executes(ctx -> executeRtxColorSet(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "red"),
                                                IntegerArgumentType.getInteger(ctx, "green"),
                                                IntegerArgumentType.getInteger(ctx, "blue") ) ) ) ) ); LiteralArgumentBuilder<FabricClientCommandSource>

rain = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)ClientCommandManager.literal
("rain" ) .executes
(ctx -> executeRain(ctx, 1))) .then
 (ClientCommandManager.argument("size", IntegerArgumentType.integer(1, 10)) .executes
 (ctx -> executeRain(ctx, IntegerArgumentType.getInteger

 (ctx, "size"))) );
        LiteralArgumentBuilder<FabricClientCommandSource>
blackHole = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal
("blackhole" ) .executes
(ctx -> executeBlackHole(ctx, 1)))
                .then(ClientCommandManager.literal("delete").executes
(ctx -> executeBlackHoleDelete(ctx))))
            .then(ClientCommandManager.argument("size", IntegerArgumentType.integer(1, 10))
                    .executes(ctx -> executeBlackHole(ctx, IntegerArgumentType.getInteger

 (ctx, "size"))) );
        LiteralArgumentBuilder<FabricClientCommandSource>
airDistortion = (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal
("airdistortion" ) .executes
(ctx -> executeAirDistortion(ctx, 1)))
                    .then(ClientCommandManager.literal("explosion")
                            .then(ClientCommandManager.argument("position", Vec3Argument.vec3())
                                    .then(ClientCommandManager.argument("power", IntegerArgumentType.integer(1, 40))
                                            .then(ClientCommandManager.argument("speed", IntegerArgumentType.integer(1, 300))
                                                    .executes(ctx -> executeAirDistortionExplosion(
                                                                ctx,
                                                                ctx.getArgument("position", Coordinates.class)
                                                                    .getPosition(((FabricClientCommandSource)ctx.getSource()).getPlayer().createCommandSourceStack() ), IntegerArgumentType.getInteger
 (ctx, "power"),
                                                                IntegerArgumentType.getInteger(ctx, "speed") ) ) ) ) ) )) .then
 (ClientCommandManager.literal("delete").executes
(ctx -> executeAirDistortionDelete(ctx)))) .then
(ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 10)) .executes
 (ctx -> executeAirDistortion(ctx, IntegerArgumentType.getInteger

 (ctx, "radius"))) );
        LiteralArgumentBuilder<FabricClientCommandSource>
explosionXul = (LiteralArgumentBuilder<FabricClientCommandSource>)ClientCommandManager.literal
("explosionxul" ) .then
(ClientCommandManager.argument("position", Vec3Argument.vec3())
                    .then(ClientCommandManager.argument("power", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> executeExplosionXul(
                                        ctx,
                                        ctx.getArgument("position", Coordinates.class)
                                            .getPosition(((FabricClientCommandSource)ctx.getSource()).getPlayer().createCommandSourceStack()),
                                        IntegerArgumentType.getInteger(ctx, "power") ) ) ) );

dispatcher.register(skylightTest);
dispatcher.register(flashlight);
dispatcher.register(grass);
        dispatcher.register(rtxColor);
        dispatcher.register(rain);
        dispatcher.register(blackHole);
        dispatcher.register(airDistortion);
        dispatcher.register(explosionXul);
    }

    private static int executeSkylightTest(CommandContext<FabricClientCommandSource> ctx) {
        int skyLight = skyvolumetrigodrays.sampleCameraSkylight();
        if (skyLight < 0) {
            ctx.getSource().sendError(Component.literal("Skylight test requires an active client world"));
            return 0;
        } else {

            ctx.getSource().sendFeedback(Component.literal("Volumetric effective camera light: " + skyLight + "/15"));


            return 1;
        }
    }

    private static int executeFlashlightOn(CommandContext<FabricClientCommandSource> ctx, int power, int angle) {
        TestFlashLight.enable(power, angle);
        ctx.getSource() .sendFeedback(Component.literal(
                    "Flashlight enabled: power=" + TestFlashLight.getPower()
                        + ", angle=" + TestFlashLight.getConeAngle()
                        + ", rays=" + TestFlashLight.getCurrentRayCount() ) );

return 1;
    }

    private static int executeFlashlightOff(CommandContext<FabricClientCommandSource> ctx) {
        TestFlashLight.disable();
        ctx.getSource().sendFeedback(Component.literal("Flashlight disabled"));
        return 1;
    }

    private static int executeGrassOn(CommandContext<FabricClientCommandSource> ctx, int radius) {
        Grass.enable();
        Grass.setRenderRadius(radius);
        ctx.getSource().sendFeedback(Component.literal("Grass rendering enabled (radius=" + Grass.getRenderRadius
 () + " blocks)"));

        return 1;
    }

    private static int executeGrassOff(CommandContext<FabricClientCommandSource> ctx) {
        Grass.disable();
        ctx.getSource().sendFeedback(Component.literal("Grass rendering disabled"));
        return 1;
    }

    private static int executeRtxColorSet(CommandContext<FabricClientCommandSource> ctx, int red, int green, int blue) {
        Coloredlight.setColor(red, green, blue);
        ctx.getSource() .sendFeedback(Component.literal(
                    "RTX block light color set to RGB(" + Coloredlight.getRed
 () + ", " + Coloredlight.getGreen
 () + ", " + Coloredlight.getBlue
 () + ")" ) );

 return 1;
    }

    private static int executeRtxColorReset(CommandContext<FabricClientCommandSource> ctx) {
        Coloredlight.reset();
        ctx.getSource().sendFeedback(Component.literal("RTX block light color reset to vanilla"));
        return 1;
    }

    private static int executeRtxColorSpectre(CommandContext<FabricClientCommandSource> ctx, int speed) {
        Coloredlight.enableSpectre(speed);
        ctx.getSource().sendFeedback(Component.literal("§aRTX Color Spectre enabled with speed: §e" + speed));


        return 1;
    }

    private static int executeRtxColorOff(CommandContext<FabricClientCommandSource> ctx) {
        Coloredlight.disableSpectre();
        ctx.getSource().sendFeedback(Component.literal("§cRTX Color disabled"));
        return 1;
    }

    private static int executeRtxColorPolice(CommandContext<FabricClientCommandSource> ctx) {
        Coloredlight.enablePolice();
        ctx.getSource().sendFeedback(Component.literal("§9§lPOLICE MODE ACTIVATED §c§l[WEE WOO WEE WOO]"));


        return 1;
    }

    private static int executeRtxColorPolice2(CommandContext<FabricClientCommandSource> ctx) {
        Coloredlight.enablePolice2();
        ctx.getSource().sendFeedback(Component.literal("§9§lPOLICE MODE 2 ACTIVATED §c§l[FLASH FLASH FLASH]"));


        return 1;
    }

    private static int executeRain(CommandContext<FabricClientCommandSource> ctx, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            BlockPos targetPos = BlockPos.containing(mc.player.getX(), mc.player.getBoundingBox().minY - 0.05, mc.player.getZ());
            BlockState state = mc.level.getBlockState(targetPos);
            ReflectionMaterial material = ReflectionMaterialRegistry.find(state);
            if (material != null && material.shouldReflectFace(mc, targetPos, state, Direction.UP)) {
                RainPuddleSurface surface = RainPuddleManager.get().placeRandomPuddle(mc, targetPos, size);
                if (surface == null) {
                    ctx.getSource().sendError(Component.literal("No rain heightmaps found or puddle footprint has no reflective neighbors"));
                    return 0;
                } else {
                    ctx.getSource()
                        .sendFeedback(
                            Component.literal(
                                "Rain puddle placed: map="
                                    + surface.getHeightmap().getDisplayName()
                                    + ", size="
                                    + surface.getSpanBlocks()
                                    + ", reflective blocks="
                                    + surface.getCoveredBlockCount() ) );
                    return 1;
                }
            } else {
                ctx.getSource().sendError(Component.literal("Block under player is not a supported reflective top face"));
                return 0;
            }
        } else {
            ctx.getSource().sendError(Component.literal("Rain puddles require an active client world"));
            return 0;
        }
    }

    private static int executeBlackHole(CommandContext<FabricClientCommandSource> ctx, int radius) {
        Minecraft mc = Minecraft.getInstance



 ();
        if (mc.player == null) {
            ctx.getSource().sendError(Component.literal("Player is not available"));
            return 0;
        } else {

            BlackHole.spawnAtPlayer(radius);
            ctx.getSource().sendFeedback(Component.literal("Black hole spawned at your position (radius=" + radius + ")"));


            return 1;
        }
    }

    private static int executeBlackHoleDelete(CommandContext<FabricClientCommandSource> ctx) { BlackHole.clear();
        ctx.getSource().sendFeedback(Component.literal("All black holes removed"));
        return 1;
    }

    private static int executeAirDistortion(CommandContext<FabricClientCommandSource> ctx, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ctx.getSource().sendError(Component.literal("Player is not available"));
            return 0;
        } else {

            AirDistortion.spawnAtPlayer(radius);
            ctx.getSource().sendFeedback(Component.literal("Air distortion spawned at your position (radius=" + radius + ")"));


            return 1;
        }
    }

    private static int executeAirDistortionDelete(CommandContext<FabricClientCommandSource> ctx) { AirDistortion.clear();
        ShockWaveTest.clear();
        ctx.getSource().sendFeedback(Component.literal("All air distortions removed"));
        return 1;
    }

    private static int executeAirDistortionExplosion(CommandContext<FabricClientCommandSource> ctx, Vec3 position, int power, int speed) {
        ShockWaveTest.spawnExplosion(position, power, speed);
        ctx.getSource() .sendFeedback(Component.literal(
                    "Air distortion shockwave spawned at "
                        + formatCoordinate(position.x)
                        + " " + formatCoordinate(position.y)
                        + " " + formatCoordinate(position.z)
                        + " (power=" + power + ", speed=" + speed + " blocks/s)" ) );

 return 1;
    }

    private static int executeExplosionXul(CommandContext<FabricClientCommandSource> ctx, Vec3 position, int power) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ctx.getSource().sendError(Component.literal("Explosion simulation requires an active client world"));
            return 0;
        } else {
            ExplosionWorld.get().trigger(position, power);
            ctx.getSource() .sendFeedback(Component.literal(
                        "Explosion XUL spawned at "
                            + formatCoordinate(position.x)
                            + " " + formatCoordinate(position.y)
                            + " " + formatCoordinate(position.z)
                            + " (power=" + power + ")" ) );

 return 1;
        }
    }

    private static String formatCoordinate(double value) { return String.format(Locale.ROOT, "%.2f", value);
    }
    }
