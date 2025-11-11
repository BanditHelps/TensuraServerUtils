package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.api.skills.SkillAPI;
import com.github.manasmods.tensura.ability.skill.Skill;
import com.github.manasmods.tensura.capability.ep.ITensuraEPCapability;
import com.github.manasmods.tensura.capability.ep.TensuraEPCapability;
import com.github.manasmods.tensura.capability.race.ITensuraPlayerCapability;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

import java.util.*;

public class LeaderboardCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
				Commands.literal("leaderboard")
						.executes(LeaderboardCommand::openLeaderboardFromCache)
						.then(Commands.literal("test")
								.requires(source -> source.hasPermission(2))
								.then(Commands.literal("add")
										.then(Commands.argument("name", StringArgumentType.word())
												.then(Commands.argument("ep", DoubleArgumentType.doubleArg(0))
														.executes(LeaderboardCommand::addTestEntry))))
								.then(Commands.literal("remove")
										.then(Commands.argument("name", StringArgumentType.word())
												.executes(LeaderboardCommand::removeTestEntry))))
						.then(Commands.literal("debug")
								.requires(source -> source.hasPermission(2))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
										.executes(ctx -> openLeaderboardDebug(ctx, IntegerArgumentType.getInteger(ctx, "count"))))
								.executes(ctx -> openLeaderboardDebug(ctx, 60))
						)
        );
    }

	private static int openLeaderboardFromCache(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer sender = context.getSource().getPlayerOrException();

			List<LeaderboardMenu.Entry> entries = new ArrayList<>(LeaderboardCache.getSnapshot());
			if (entries.isEmpty()) {
				ServerLevel level = sender.getLevel();
				for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
					double ep = TensuraEPCapability.getFrom(p).map(d -> d.getEP()).orElse(0.0);
					boolean isDemonLord = TensuraPlayerCapability.getFrom(p).map(ITensuraPlayerCapability::isTrueDemonLord).orElse(false);
					boolean isTrueHero = TensuraPlayerCapability.getFrom(p).map(ITensuraPlayerCapability::isTrueHero).orElse(false);

					Race race = TensuraPlayerCapability.getFrom(p).map(ITensuraPlayerCapability::getRace).orElse(TensuraRaces.HUMAN.get());
					String raceName = Component.translatable(race.getNameTranslationKey()).getString();

					// Count all the unique skills
					Collection<ManasSkillInstance> skills = SkillAPI.getSkillsFrom(p).getLearnedSkills();
					int uniqueCount = 0;
					for (ManasSkillInstance skill : skills) {
						if (skill.getSkill() instanceof Skill skill1) {
							if (skill1.getType().equals(Skill.SkillType.UNIQUE)) {
								uniqueCount++;
							}
						}
					}

					// Check to see if they are "named" if so, use that name, otherwise their default player name
					String name = TensuraEPCapability.getFrom(p).map(ITensuraEPCapability::getName).orElse(p.getGameProfile().getName());

					entries.add(new LeaderboardMenu.Entry(name, p.getUUID(), ep, raceName, uniqueCount, isDemonLord, isTrueHero));
				}
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
				String race = Component.translatable(TensuraRaces.HUMAN.get().getNameTranslationKey()).getString();
				entries.add(new LeaderboardMenu.Entry(name, new UUID(0L, i), ep, race, 0, false, false));
			}
			entries.sort(Comparator.comparingDouble((LeaderboardMenu.Entry e) -> e.ep).reversed());
			openMenu(sender, entries);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
			return 0;
		}
	}

	private static int addTestEntry(CommandContext<CommandSourceStack> context) {
		try {
			String name = StringArgumentType.getString(context, "name");
			double ep = DoubleArgumentType.getDouble(context, "ep");
			LeaderboardCache.putTestEntry(name, ep);
			LeaderboardCache.saveToDisk(context.getSource().getServer());
			context.getSource().sendSuccess(Component.literal("Added test entry: " + name + " with EP " + ep), true);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
			return 0;
		}
	}

	private static int removeTestEntry(CommandContext<CommandSourceStack> context) {
		try {
			String name = StringArgumentType.getString(context, "name");
			boolean removed = LeaderboardCache.removeTestEntry(name);
			if (removed) {
				LeaderboardCache.saveToDisk(context.getSource().getServer());
				context.getSource().sendSuccess(Component.literal("Removed test entry: " + name), true);
				return 1;
			} else {
				context.getSource().sendFailure(Component.literal("No test entry found for name: " + name));
				return 0;
			}
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
