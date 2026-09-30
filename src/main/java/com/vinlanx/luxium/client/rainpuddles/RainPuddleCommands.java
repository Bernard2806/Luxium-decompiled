/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.client.Minecraft
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterClientCommandsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.client.rainpuddles;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleInstance;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class RainPuddleCommands {
    private RainPuddleCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"rainnew").executes(ctx -> RainPuddleCommands.place((CommandSourceStack)ctx.getSource(), 1))).then(Commands.m_82129_((String)"size", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10)).executes(ctx -> RainPuddleCommands.place((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"size")))));
    }

    private static int place(CommandSourceStack source, int size) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            source.m_81352_((Component)Component.m_237113_((String)"New rain puddles require an active client world"));
            return 0;
        }
        BlockPos target = BlockPos.m_274561_((double)mc.f_91074_.m_20185_(), (double)(mc.f_91074_.m_20191_().f_82289_ - 0.05), (double)mc.f_91074_.m_20189_());
        RainPuddleInstance puddle = RainPuddleManager.get().placeRandom(mc, target, size);
        if (puddle == null) {
            source.m_81352_((Component)Component.m_237113_((String)"Could not place a puddle here or no rain depth maps were found"));
            return 0;
        }
        source.m_288197_(() -> Component.m_237113_((String)("New rain puddle placed: map=" + puddle.depthMap().m_135815_() + ", size=" + puddle.sizeBlocks() + ", surface patches=" + puddle.patchCount())), false);
        return 1;
    }
}

