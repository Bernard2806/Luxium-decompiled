package com.vinlanx.luxium.client.rainpuddles; import com.mojang.brigadier.arguments.ArgumentType; import com.mojang.brigadier.arguments.IntegerArgumentType; import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleInstance;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
 public final class RainPuddleCommands { private RainPuddleCommands() {
 }

    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {

        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal
                ((String)"rainnew").executes
                (ctx -> RainPuddleCommands.place((CommandSourceStack)ctx.getSource(), 1))).then
                (Commands.argument((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes
                (ctx -> RainPuddleCommands.place((CommandSourceStack)ctx.getSource
                (), IntegerArgumentType.getInteger
                ((CommandContext)ctx, (String)"size")))));
    }

    private static int place(CommandSourceStack source, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            source.sendFailure((Component)Component.literal((String)"New rain puddles require an active client world"));
            return 0;
        }

        BlockPos target = BlockPos.containing((double)mc.player.getX
                (), (double)(mc.player.getBoundingBox
                ().minY - 0.05), (double)mc.player.getZ
                ());

        RainPuddleInstance puddle = RainPuddleManager.get().placeRandom(mc, target, size);
        if (puddle == null) {
            source.sendFailure((Component)Component.literal((String)"Could not place a puddle here or no rain depth maps were found"));

            return 0;
        }

        source.sendSuccess(() -> Component.literal((String)("New rain puddle placed: map=" + puddle.depthMap
                ().getPath() + ", size=" + puddle.sizeBlocks
                () + ", surface patches=" + puddle.patchCount
                ())), false);
        return 1;
    }
    }
