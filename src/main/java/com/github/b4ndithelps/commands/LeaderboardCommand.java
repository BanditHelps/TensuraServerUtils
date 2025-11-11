package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import com.github.manasmods.tensura.capability.ep.TensuraEPCapability;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LeaderboardCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
				Commands.literal("leaderboard")
						.executes(LeaderboardCommand::openLeaderboardFromOnlinePlayers)
						.then(Commands.literal("debug")
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
										.executes(ctx -> openLeaderboardDebug(ctx, IntegerArgumentType.getInteger(ctx, "count"))))
								.executes(ctx -> openLeaderboardDebug(ctx, 60))
						)
        );
    }

	private static int openLeaderboardFromOnlinePlayers(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer sender = context.getSource().getPlayerOrException();
			ServerLevel level = sender.getLevel();

			List<LeaderboardMenu.Entry> entries = new ArrayList<>();
			for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
				double ep = TensuraEPCapability.getFrom(p).map(d -> d.getEP()).orElse(0.0);
				Race race = TensuraPlayerCapability.getFrom(p).map(d -> d.getRace()).orElse(TensuraRaces.HUMAN.get());
				entries.add(new LeaderboardMenu.Entry(p.getGameProfile().getName(), p.getUUID(), ep, race));
			}

			// Sort desc to ensure consistent display order
			entries.sort(Comparator.comparingDouble((LeaderboardMenu.Entry e) -> e.ep).reversed());
			openMenu(sender, entries);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

	private static int openLeaderboardDebug(CommandContext<CommandSourceStack> context, int count) {
		try {
			ServerPlayer sender = context.getSource().getPlayerOrException();
			List<LeaderboardMenu.Entry> entries = new ArrayList<>(count);
			for (int i = 1; i <= count; i++) {
				double ep = Math.max(1, (count - i + 1) * 1000L);
				String name = "Player" + i;
				Race race = TensuraRaces.HUMAN.get();
				entries.add(new LeaderboardMenu.Entry(name, new UUID(0L, i), ep, race));
			}
			entries.sort(Comparator.comparingDouble((LeaderboardMenu.Entry e) -> e.ep).reversed());
			openMenu(sender, entries);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
			return 0;
		}
	}

	private static void openMenu(ServerPlayer player, List<LeaderboardMenu.Entry> entries) {
		MenuProvider provider = new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Leaderboard");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
				return new LeaderboardMenu(id, playerInventory, new SimpleContainer(LeaderboardMenu.SIZE), getDisplayName(), entries);
			}
		};
		player.openMenu(provider);
	}
}
