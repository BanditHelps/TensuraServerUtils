package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.menus.LeaderboardMenu;
import com.github.manasmods.tensura.race.Race;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LeaderboardUtil {
	public static ItemStack createHead(LeaderboardMenu.Entry entry, int place) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		// Display name with place
		Component name = Component.literal("#" + place + " " + entry.name);
		stack.setHoverName(name);
		// Owner (by name if available)
		CompoundTag tag = stack.getOrCreateTag();
		if (entry.name != null && !entry.name.isEmpty()) {
			tag.putString("SkullOwner", entry.name);
		}
		// Lore with EP
		CompoundTag display = stack.getOrCreateTagElement("display");
		ListTag lore = new ListTag();
		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.literal("EP: ")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
						.append(Component.literal(formatEp(entry.ep)).withStyle(ChatFormatting.AQUA))
		)));
		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.translatable(formatRace(entry.race)).withStyle(ChatFormatting.GOLD))
		));
		display.put("Lore", lore);
		return stack;
	}

	private static String formatEp(double ep) {
		if (ep >= 1_000_000_000) return String.format("%.2fB", ep / 1_000_000_000d);
		if (ep >= 1_000_000) return String.format("%.2fM", ep / 1_000_000d);
		if (ep >= 1_000) return String.format("%.1fk", ep / 1_000d);
		return String.format("%.0f", ep);
	}

	private static String formatRace(Race race) {
		return race.getNameTranslationKey();
	}
}


