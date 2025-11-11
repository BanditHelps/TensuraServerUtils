package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeaderboardSavedData extends SavedData {
	private static final String DATA_NAME = "trutils_leaderboard";

	private final Map<UUID, PlayerRecord> records = new HashMap<>();

	public LeaderboardSavedData() {
	}

	public static LeaderboardSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(LeaderboardSavedData::load, LeaderboardSavedData::new, DATA_NAME);
	}

	public static LeaderboardSavedData load(CompoundTag tag) {
		LeaderboardSavedData data = new LeaderboardSavedData();
		ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
		for (Tag t : list) {
			CompoundTag e = (CompoundTag) t;
			try {
				UUID uuid = e.hasUUID("uuid") ? e.getUUID("uuid") : UUID.fromString(e.getString("uuid_str"));
				String name = e.getString("name");
				double ep = e.getDouble("ep");
				data.records.put(uuid, new PlayerRecord(name, ep));
			} catch (Exception ignored) {
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, PlayerRecord> entry : records.entrySet()) {
			CompoundTag e = new CompoundTag();
			e.putUUID("uuid", entry.getKey());
			e.putString("name", entry.getValue().name);
			e.putDouble("ep", entry.getValue().ep);
			list.add(e);
		}
		tag.put("entries", list);
		return tag;
	}

	public Map<UUID, PlayerRecord> getRecords() {
		return records;
	}

	public void replaceFromCache(Map<UUID, LeaderboardMenu.Entry> cache) {
		boolean changed = false;

		// Quick checks: size or any missing keys
		if (records.size() != cache.size()) {
			changed = true;
		} else {
			for (Map.Entry<UUID, LeaderboardMenu.Entry> e : cache.entrySet()) {
				PlayerRecord rec = records.get(e.getKey());
				if (rec == null) { changed = true; break; }
				if (!rec.name.equals(e.getValue().name) || Double.compare(rec.ep, e.getValue().ep) != 0) {
					changed = true;
					break;
				}
			}
		}

		if (!changed) return;

		this.records.clear();
		for (Map.Entry<UUID, LeaderboardMenu.Entry> e : cache.entrySet()) {
			this.records.put(e.getKey(), new PlayerRecord(e.getValue().name, e.getValue().ep));
		}
		setDirty();
	}

	public static class PlayerRecord {
		public final String name;
		public final double ep;

		public PlayerRecord(String name, double ep) {
			this.name = name;
			this.ep = ep;
		}
	}
}


