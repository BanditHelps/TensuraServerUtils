package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import com.github.b4ndithelps.trutils.Trutils;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.api.skills.SkillAPI;
import com.github.manasmods.tensura.ability.skill.Skill;
import com.github.manasmods.tensura.capability.ep.ITensuraEPCapability;
import com.github.manasmods.tensura.capability.ep.TensuraEPCapability;
import com.github.manasmods.tensura.capability.race.ITensuraPlayerCapability;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

@Mod.EventBusSubscriber(modid = Trutils.MODID)
public class LeaderboardCache {
	private static final Map<UUID, LeaderboardMenu.Entry> uuidToEntry = new ConcurrentHashMap<>();
	private static int ticksSinceRefresh = 0;
	private static final int REFRESH_TICKS = 20 * 30; // 30 seconds at 20 TPS

	public static List<LeaderboardMenu.Entry> getSnapshot() {
		return new ArrayList<>(uuidToEntry.values());
	}

	public static void loadFromDisk(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (overworld == null) return;
		LeaderboardSavedData data = LeaderboardSavedData.get(overworld);
		uuidToEntry.clear();
		for (Map.Entry<UUID, LeaderboardSavedData.PlayerRecord> e : data.getRecords().entrySet()) {
			uuidToEntry.put(e.getKey(), new LeaderboardMenu.Entry(e.getValue().name, e.getKey(), e.getValue().ep, e.getValue().race, e.getValue().uniqueCount, e.getValue().isDemonLord, e.getValue().isTrueHero));
		}
	}

	public static void saveToDisk(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (overworld == null) return;
		LeaderboardSavedData data = LeaderboardSavedData.get(overworld);
		data.replaceFromCache(uuidToEntry);
	}

	/**
	 * Adds or updates a test entry. The UUID is derived deterministically from the name
	 * and prefixed to avoid collision with real player UUIDs.
	 */
	public static void putTestEntry(String name, double ep) {
		UUID uuid = UUID.nameUUIDFromBytes(("trutils-test:" + name).getBytes(StandardCharsets.UTF_8));
		String race = Component.translatable(TensuraRaces.HUMAN.get().getNameTranslationKey()).getString();
		uuidToEntry.put(uuid, new LeaderboardMenu.Entry(name, uuid, ep, race, 0, false, false));
	}

	/**
	 * Removes a test entry by name. Returns true if removed.
	 */
	public static boolean removeTestEntry(String name) {
		UUID uuid = UUID.nameUUIDFromBytes(("trutils-test:" + name).getBytes(StandardCharsets.UTF_8));
		return uuidToEntry.remove(uuid) != null;
	}

	public static void refresh(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer p : players) {
			try {
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

				String name;
				// Check to see if they are "named" if so, use that name, otherwise their default player name
				try {
					name = TensuraEPCapability.getFrom(p).map(ITensuraEPCapability::getName).orElse(p.getGameProfile().getName());
				} catch(Exception e) {
					name = p.getGameProfile().getName();
				}

				uuidToEntry.put(p.getUUID(), new LeaderboardMenu.Entry(name, p.getUUID(), ep, raceName, uniqueCount, isDemonLord, isTrueHero));
			} catch (Exception e) {
				Trutils.LOGGER.warn("One of the following failed: ", e.getMessage());
				return;
			}
		}
		saveToDisk(server);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
		ticksSinceRefresh++;
		if (ticksSinceRefresh >= REFRESH_TICKS) {
			ticksSinceRefresh = 0;
			refresh(event.getServer());
		}
	}
}