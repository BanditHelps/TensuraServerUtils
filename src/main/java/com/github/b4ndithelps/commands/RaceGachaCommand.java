package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.RaceSlotMachineMenu;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;

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
		MenuProvider provider = new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Race Selection");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
				return new RaceSlotMachineMenu(id, playerInventory, new SimpleContainer(RaceSlotMachineMenu.SIZE), getDisplayName(), player);
			}
		};
		player.openMenu(provider);
	}
}