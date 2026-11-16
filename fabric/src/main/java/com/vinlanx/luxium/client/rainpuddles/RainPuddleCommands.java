package com.vinlanx.luxium.client.rainpuddles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class RainPuddleCommands {
    private RainPuddleCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            (LiteralArgumentBuilder<FabricClientCommandSource>)((LiteralArgumentBuilder)ClientCommandManager.literal("rainnew")
                    .executes(ctx -> place((FabricClientCommandSource)ctx.getSource(), 1)))
                .then(ClientCommandManager.argument("size", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> place((FabricClientCommandSource)ctx.getSource
(), IntegerArgumentType.getInteger
 (ctx, "size"))) ) );
}

    private static int place(FabricClientCommandSource source, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player != null && mc.level != null)) {
            source.sendError(Component.literal("New rain puddles require an active client world"));
            return 0;
        }
            BlockPos
            target = BlockPos.containing(mc.player.getX
(), mc.player.getBoundingBox
().minY - 0.05, mc.player.getZ
());
            RainPuddleInstance puddle = RainPuddleManager.get
 ().placeRandom(mc, target, size);
            if (puddle == null) {
                source.sendError(Component.literal("Could not place a puddle here or no rain depth maps were found"));

                return 0;
            } else {

                source.sendFeedback(Component.literal(
                        "New rain puddle placed: map=" + puddle.depthMap().getPath()
                            + ", size=" + puddle.sizeBlocks()
                            + ", surface patches=" + puddle.patchCount() ) );
                return 1;
            }

    }
    }
