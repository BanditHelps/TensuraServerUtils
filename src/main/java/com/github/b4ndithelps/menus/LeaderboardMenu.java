package com.github.b4ndithelps.menus;

import com.github.b4ndithelps.commands.LeaderboardUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LeaderboardMenu extends ChestMenu {
	public static final int ROWS = 6;
	public static final int COLUMNS = 9;
	public static final int SIZE = ROWS * COLUMNS;
	public static final int SLOT_PREV = (ROWS - 1) * COLUMNS; // 45
	public static final int SLOT_NEXT = (ROWS * COLUMNS) - 1; // 53

	private final Container leaderboardContainer;
	private final Component title;
	private final List<Entry> entries;
	private int currentPage;

	// Client-side constructor (no data needed; slots sync from server)
	public LeaderboardMenu(int id, Inventory playerInventory) {
		super(ModMenus.LEADERBOARD_MENU.get(), id, playerInventory, new SimpleContainer(SIZE), ROWS);
		this.leaderboardContainer = this.getContainer();
		this.title = Component.literal("Leaderboard");
		this.entries = new ArrayList<>();
		this.currentPage = 0;
	}

	// Server-side constructor
	public LeaderboardMenu(int id, Inventory playerInventory, Container container, Component title, List<Entry> entries) {
		super(ModMenus.LEADERBOARD_MENU.get(), id, playerInventory, container, ROWS);
		this.leaderboardContainer = container;
		this.title = title;
		this.entries = new ArrayList<>(entries);
		this.currentPage = 0;
		populate();
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
		if (slotId == SLOT_PREV) {
			previousPage();
			broadcastChanges();
			return;
		}
		if (slotId == SLOT_NEXT) {
			nextPage();
			broadcastChanges();
			return;
		}
		// Prevent taking/moving any items from our GUI area
		if (slotId >= 0 && slotId < SIZE) {
			return;
		}
		super.clicked(slotId, dragType, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		// Disable shift-click transfers from/to our GUI
		return ItemStack.EMPTY;
	}

	private void previousPage() {
		int maxPage = Math.max(0, getTotalPages() - 1);
		if (currentPage <= 0) {
			currentPage = maxPage;
		} else {
			currentPage--;
		}
		populate();
	}

	private void nextPage() {
		int maxPage = Math.max(0, getTotalPages() - 1);
		if (currentPage >= maxPage) {
			currentPage = 0;
		} else {
			currentPage++;
		}
		populate();
	}

	private int getTotalPages() {
		int others = Math.max(0, entries.size() - 3);
		int pageSize = 3 * 9; // rows 1..4 = 36
		return (int) Math.ceil(others / (double) pageSize);
	}

	private void populate() {
		// Clear all
		for (int i = 0; i < SIZE; i++) {
			leaderboardContainer.setItem(i, ItemStack.EMPTY);
		}
		if (entries.isEmpty()) {
			return;
		}
		// Ensure sorted by EP desc
		entries.sort(Comparator.comparingDouble((Entry e) -> e.ep).reversed());

		// Top 3 in slots [4,3,5] if present
		int[] topSlots = new int[]{4, 12, 14};
		for (int i = 0; i < Math.min(3, entries.size()); i++) {
			Entry e = entries.get(i);
			ItemStack head = LeaderboardUtil.createHead(e, i + 1);
			leaderboardContainer.setItem(topSlots[i], head);
		}

		// Others into rows 1..4 (slots 9..44)
		int pageSize = 3 * 9;
		int othersStartIndex = 3;
		int totalOthers = Math.max(0, entries.size() - othersStartIndex);
		int totalPages = Math.max(1, (int) Math.ceil(totalOthers / (double) pageSize));
		int page = Math.min(currentPage, totalPages - 1);
		this.currentPage = page;
		int start = othersStartIndex + page * pageSize;
		int end = Math.min(entries.size(), start + pageSize);

		int slot = 18; // start of row 1
		for (int i = start; i < end; i++) {
			Entry e = entries.get(i);
			ItemStack head = LeaderboardUtil.createHead(e, i + 1);
			leaderboardContainer.setItem(slot++, head);
		}

		// Nav items if multiple pages
		if (totalPages > 1) {
			ItemStack prev = new ItemStack(Items.ARROW);
			prev.setHoverName(Component.literal("Previous Page (" + (page + 1) + "/" + totalPages + ")"));
			ItemStack next = new ItemStack(Items.ARROW);
			next.setHoverName(Component.literal("Next Page (" + (page + 1) + "/" + totalPages + ")"));
			leaderboardContainer.setItem(SLOT_PREV, prev);
			leaderboardContainer.setItem(SLOT_NEXT, next);
		} else {
			// Single page, but still show disabled buttons for layout clarity
			ItemStack left = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
			left.setHoverName(Component.literal("No other pages"));
			ItemStack right = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
			right.setHoverName(Component.literal("No other pages"));
			leaderboardContainer.setItem(SLOT_PREV, left);
			leaderboardContainer.setItem(SLOT_NEXT, right);
		}
	}

	public static class Entry {
		public final String name;
		public final UUID uuid;
		public final double ep;
		public final String race;
		public final int uniqueCount;
		public final boolean isDemonLord;
		public final boolean isTrueHero;

		public Entry(String name, UUID uuid, double ep, String race, int uniqueCount, boolean isDemonLord, boolean isTrueHero) {
			this.name = name;
			this.uuid = uuid;
			this.ep = ep;
			this.race = race;
			this.uniqueCount = uniqueCount;
			this.isDemonLord = isDemonLord;
			this.isTrueHero = isTrueHero;
		}
	}
}