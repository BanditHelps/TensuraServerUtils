package com.github.b4ndithelps.commands;

import com.github.manasmods.tensura.item.custom.ResetScrollItem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;


public class RaceGachaCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("racegacha")
						.requires(source -> source.hasPermission(2))
						.executes(ctx -> {
							ServerPlayer sender = ctx.getSource().getPlayerOrException();
							open(sender);
							return 1;
						})
		);
	}

	private static void open(ServerPlayer player) {
		if (player != null) {
			ResetScrollItem.resetRace(player);
		}
	}
}