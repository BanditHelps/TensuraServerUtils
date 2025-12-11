package com.github.b4ndithelps.trutils.config;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public final class TrutilsConfig {

	public static final ForgeConfigSpec COMMON_SPEC;

	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_SKILLS;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKILL_BYPASS_UUIDS;

	// Captured at config load to allow saving after runtime updates
	private static volatile ModConfig COMMON_CONFIG_REF;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		BLOCKED_SKILLS = builder
			.comment(
				"List of skill IDs that cannot be learned.",
				"Use the full ID in namespace:path form, e.g. \"tensura:predator\"."
			)
			.defineList("blockedSkills", Collections.emptyList(), o -> o instanceof String);

		SKILL_BYPASS_UUIDS = builder
			.comment(
				"UUIDs that bypass the blockedSkills check.",
				"Use standard UUID format; managed via /trutils skillBypass add/remove <player_name>."
			)
			.defineList("skillBypassUuids", Collections.emptyList(), o -> o instanceof String);

		COMMON_SPEC = builder.build();
	}

	private TrutilsConfig() {}

	public static boolean isSkillBlocked(ResourceLocation skillId) {
		if (skillId == null) return false;
		Set<ResourceLocation> blocked = getBlockedSkillIds();
		return blocked.contains(skillId);
	}

	public static Set<ResourceLocation> getBlockedSkillIds() {
		List<? extends String> raw = Optional.ofNullable(BLOCKED_SKILLS.get()).orElse(Collections.emptyList());
		if (raw.isEmpty()) return Collections.emptySet();

		Set<ResourceLocation> parsed = new HashSet<>();
		for (Object entry : raw) {
			if (!(entry instanceof String s)) continue;
			String trimmed = s.trim();
			if (trimmed.isEmpty()) continue;
			try {
				ResourceLocation id = ResourceLocation.tryParse(trimmed);
				if (id != null) {
					parsed.add(id);
				} else {
					Trutils.LOGGER.warn("TRUtils config: invalid skill id '{}'", trimmed);
				}
			} catch (Exception ex) {
				Trutils.LOGGER.warn("TRUtils config: failed to parse skill id '{}': {}", trimmed, ex.getMessage());
			}
		}
		return parsed;
	}

	public static Set<UUID> getBypassUuids() {
		List<? extends String> raw = Optional.ofNullable(SKILL_BYPASS_UUIDS.get()).orElse(Collections.emptyList());
		if (raw.isEmpty()) return Collections.emptySet();
		Set<UUID> parsed = new HashSet<>();
		for (Object entry : raw) {
			if (!(entry instanceof String s)) continue;
			String trimmed = s.trim();
			if (trimmed.isEmpty()) continue;
			try {
				parsed.add(UUID.fromString(trimmed));
			} catch (Exception ex) {
				Trutils.LOGGER.warn("TRUtils config: invalid bypass UUID '{}'", trimmed);
			}
		}
		return parsed;
	}

	public static boolean isUuidBypassed(UUID uuid) {
		if (uuid == null) return false;
		return getBypassUuids().contains(uuid);
	}

	public static boolean addBypassUuid(UUID uuid) {
		if (uuid == null) return false;
		List<String> list = new ArrayList<>();
		List<? extends String> current = SKILL_BYPASS_UUIDS.get();
		if (current != null) {
			for (Object o : current) {
				if (o instanceof String s) list.add(s);
			}
		}
		String s = uuid.toString();
		if (list.contains(s)) return false;
		list.add(s);
		SKILL_BYPASS_UUIDS.set(list);
		saveCommonConfig();
		return true;
	}

	public static boolean removeBypassUuid(UUID uuid) {
		if (uuid == null) return false;
		List<String> list = new ArrayList<>();
		boolean removed = false;
		List<? extends String> current = SKILL_BYPASS_UUIDS.get();
		if (current != null) {
			for (Object o : current) {
				if (o instanceof String s) list.add(s);
			}
		}
		String s = uuid.toString();
		removed = list.remove(s);
		if (removed) {
			SKILL_BYPASS_UUIDS.set(list);
			saveCommonConfig();
		}
		return removed;
	}

	private static void saveCommonConfig() {
		ModConfig cfg = COMMON_CONFIG_REF;
		if (cfg != null && cfg.getType() == ModConfig.Type.COMMON) {
			try {
				cfg.save();
			} catch (Exception ex) {
				Trutils.LOGGER.warn("TRUtils config: failed to save COMMON config: {}", ex.getMessage());
			}
		}
	}

	@Mod.EventBusSubscriber(modid = Trutils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ConfigEvents {
		@SubscribeEvent
		public static void onConfigLoading(ModConfigEvent.Loading event) {
			if (event.getConfig() != null && Trutils.MODID.equals(event.getConfig().getModId()) && event.getConfig().getType() == ModConfig.Type.COMMON) {
				COMMON_CONFIG_REF = event.getConfig();
			}
		}

		@SubscribeEvent
		public static void onConfigReloading(ModConfigEvent.Reloading event) {
			if (event.getConfig() != null && Trutils.MODID.equals(event.getConfig().getModId()) && event.getConfig().getType() == ModConfig.Type.COMMON) {
				COMMON_CONFIG_REF = event.getConfig();
			}
		}
	}
}
