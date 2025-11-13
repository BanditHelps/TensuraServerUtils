package com.github.b4ndithelps.trutils.config;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TrutilsConfig {

	public static final ForgeConfigSpec COMMON_SPEC;

	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_SKILLS;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		BLOCKED_SKILLS = builder
			.comment(
				"List of skill IDs that cannot be learned.",
				"Use the full ID in namespace:path form, e.g. \"tensura:predator\"."
			)
			.defineList("blockedSkills", Collections.emptyList(), o -> o instanceof String);

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
}