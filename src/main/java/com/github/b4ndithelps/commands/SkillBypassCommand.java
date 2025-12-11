package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.trutils.Trutils;
import com.github.b4ndithelps.trutils.config.TrutilsConfig;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;

import java.util.Optional;
import java.util.UUID;

public class SkillBypassCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("trutils")
				.requires(source -> source.hasPermission(2))
				.then(
					Commands.literal("skillBypass")
						.then(
							Commands.literal("add")
								.then(Commands.argument("player_name", StringArgumentType.word())
									.executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "player_name")))
								)
						)
						.then(
							Commands.literal("remove")
								.then(Commands.argument("player_name", StringArgumentType.word())
									.executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "player_name")))
								)
						)
				)
		);
	}

	private static int add(CommandSourceStack source, String playerName) {
		MinecraftServer server = source.getServer();
		Optional<UUID> maybe = resolveUuid(server, playerName);
		if (maybe.isEmpty()) {
			source.sendFailure(Component.literal("Could not resolve player '" + playerName + "' to a UUID."));
			return 0;
		}
		UUID uuid = maybe.get();
		boolean added = TrutilsConfig.addBypassUuid(uuid);
		if (added) {
			source.sendSuccess(Component.literal("Added " + playerName + " (" + uuid + ") to skill bypass."), true);
			return 1;
		} else {
			source.sendFailure(Component.literal(playerName + " (" + uuid + ") is already in the skill bypass list."));
			return 0;
		}
	}

	private static int remove(CommandSourceStack source, String playerName) {
		MinecraftServer server = source.getServer();
		Optional<UUID> maybe = resolveUuid(server, playerName);
		if (maybe.isEmpty()) {
			source.sendFailure(Component.literal("Could not resolve player '" + playerName + "' to a UUID."));
			return 0;
		}
		UUID uuid = maybe.get();
		boolean removed = TrutilsConfig.removeBypassUuid(uuid);
		if (removed) {
			source.sendSuccess(Component.literal("Removed " + playerName + " (" + uuid + ") from skill bypass."), true);
			return 1;
		} else {
			source.sendFailure(Component.literal(playerName + " (" + uuid + ") is not in the skill bypass list."));
			return 0;
		}
	}

	private static Optional<UUID> resolveUuid(MinecraftServer server, String name) {
		try {
			GameProfileCache cache = server.getProfileCache();
			if (cache != null) {
				Optional<GameProfile> cached = cache.get(name);
				if (cached.isPresent()) {
					return Optional.ofNullable(cached.get().getId());
				}
			}

			final UUID[] found = new UUID[1];
			GameProfileRepository repo = server.getProfileRepository();
			if (repo != null) {
				repo.findProfilesByNames(new String[]{name}, Agent.MINECRAFT, new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile profile) {
						try {
							if (cache != null) cache.add(profile);
						} catch (Exception ignored) {}
						found[0] = profile.getId();
					}

					@Override
					public void onProfileLookupFailed(GameProfile profile, Exception e) {
						Trutils.LOGGER.warn("Failed to lookup profile for '{}': {}", name, e.getMessage());
					}
				});
			}
			return Optional.ofNullable(found[0]);
		} catch (Exception ex) {
			Trutils.LOGGER.warn("Unexpected error resolving uuid for '{}': {}", name, ex.getMessage());
			return Optional.empty();
		}
	}
}

