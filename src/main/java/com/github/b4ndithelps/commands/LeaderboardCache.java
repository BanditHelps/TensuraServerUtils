package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import com.github.b4ndithelps.trutils.Trutils;
import com.github.manasmods.tensura.capability.ep.TensuraEPCapability;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
			uuidToEntry.put(e.getKey(), new LeaderboardMenu.Entry(e.getValue().name, e.getKey(), e.getValue().ep, TensuraRaces.HUMAN.get()));
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
		Race race = TensuraRaces.HUMAN.get();
		uuidToEntry.put(uuid, new LeaderboardMenu.Entry(name, uuid, ep, race));
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
			double ep = TensuraEPCapability.getFrom(p).map(d -> d.getEP()).orElse(0.0);
			Race race = TensuraPlayerCapability.getFrom(p).map(d -> d.getRace()).orElse(TensuraRaces.HUMAN.get());
			uuidToEntry.put(p.getUUID(), new LeaderboardMenu.Entry(p.getGameProfile().getName(), p.getUUID(), ep, race));
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