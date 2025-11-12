package com.github.b4ndithelps.menus;

import com.github.b4ndithelps.trutils.Trutils;
import com.github.manasmods.tensura.ability.TensuraSkill;
import com.github.manasmods.tensura.menu.RaceSelectionMenu;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.github.manasmods.tensura.capability.race.TensuraPlayerCapability.loadRaces;

public class RaceSlotMachineMenu extends ChestMenu {
	public static final int ROWS = 3;
	public static final int COLUMNS = 9;
	public static final int SIZE = ROWS * COLUMNS;

	private static final int SLOT_START = 22; // center bottom (row 2)
	private static final int SLOT_REEL_0 = 10; // middle row spread
	private static final int SLOT_REEL_1 = 13;
	private static final int SLOT_REEL_2 = 16;

	private final Container container;
	private final Component title;

	private final List<Race> availableRaces;
	private final ServerPlayer owner; // for sound playback

	private enum Phase { IDLE, ROLLING, CHOOSE, DONE }
	private Phase phase = Phase.IDLE;

	private int ticks;
	private final Random random = new Random();

	private int stopTick0;
	private int stopTick1;
	private int stopTick2;

	private int nextChangeTick0;
	private int nextChangeTick1;
	private int nextChangeTick2;

	private Race finalRace0;
	private Race finalRace1;
	private Race finalRace2;

	// Border animation order (exclude 22 which is our Start/Status slot)
	// Expands from bottom center outward, then up sides, then across top to meet at center (4)
	private static final int[] BORDER_PATH = new int[] {
			21, 23, 20, 24, 19, 25, 18, 26, // bottom row outward
			9, 17, 0, 8, 1, 7, 2, 6, 3, 5, 4 // sides then top toward center
	};

	// Client-side constructor (slots sync from server)
	public RaceSlotMachineMenu(int id, Inventory playerInventory) {
		super(ModMenus.RACE_SLOT_MACHINE_MENU.get(), id, playerInventory, new SimpleContainer(SIZE), ROWS);
		this.container = this.getContainer();
		this.title = Component.literal("Race Selection");
		this.availableRaces = new ArrayList<>();
		this.owner = null;
	}

	// Server-side constructor
	public RaceSlotMachineMenu(int id, Inventory playerInventory, Container container, Component title, ServerPlayer owner) {
		super(ModMenus.RACE_SLOT_MACHINE_MENU.get(), id, playerInventory, container, ROWS);
		this.container = container;
		this.title = title;
		this.availableRaces = loadAvailableRaces();
		this.owner = owner;
		populateIdle();
		RaceSlotMachineTicker.track(this);
	}

	private List<Race> loadAvailableRaces() {
		List<ResourceLocation> ids = loadRaces();
		List<Race> races = new ArrayList<>(ids.size());
		for (ResourceLocation id : ids) {
			Race r = TensuraRaces.RACE_REGISTRY.get().getValue(id);
			if (r != null) {
				races.add(r);
			}
		}
		if (races.isEmpty()) {
			races.add(TensuraRaces.HUMAN.get());
		}
		return races;
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
		if (slotId == SLOT_START) {
			if (phase == Phase.IDLE) {
				startRolling();
				broadcastChanges();
				return;
			}
			// ignore clicks on start in other phases
			return;
		}
		// During choose phase, allow selecting one of the three reels
		if (phase == Phase.CHOOSE && slotId >= 0) {
			if (slotId == SLOT_REEL_0 && finalRace0 != null) {
				applyRaceAndClose(player, finalRace0);
				return;
			}
			if (slotId == SLOT_REEL_1 && finalRace1 != null) {
				applyRaceAndClose(player, finalRace1);
				return;
			}
			if (slotId == SLOT_REEL_2 && finalRace2 != null) {
				applyRaceAndClose(player, finalRace2);
				return;
			}
			// ignore clicks within our GUI slots
			if (slotId < SIZE) {
				return;
			}
		}
		// Otherwise block moving items in our area
		if (slotId >= 0 && slotId < SIZE) {
			return;
		}
		super.clicked(slotId, dragType, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	private void populateIdle() {
		// clear
		for (int i = 0; i < SIZE; i++) {
			container.setItem(i, ItemStack.EMPTY);
		}
		// Background fill (subtle dimming), keep reels and start clear initially
		ItemStack bg = new ItemStack(Items.IRON_BARS);
		bg.setHoverName(Component.literal(" "));
		for (int i = 0; i < SIZE; i++) {
			if (i == SLOT_REEL_0 || i == SLOT_REEL_1 || i == SLOT_REEL_2 || i == SLOT_START) continue;
			container.setItem(i, bg.copy());
		}
		// Gray border baseline
		for (int slot : BORDER_PATH) {
			container.setItem(slot, grayPane(" "));
		}
		// Start button
		ItemStack start = new ItemStack(Items.LEVER);
		start.setHoverName(Component.literal("Start").withStyle(ChatFormatting.GREEN));
		addLore(start,
				Component.literal("Click to spin the reels").withStyle(ChatFormatting.YELLOW),
				Component.literal("Best of three, your choice!").withStyle(ChatFormatting.AQUA)
		);
		container.setItem(SLOT_START, start);

		// Placeholders at reel slots
		container.setItem(SLOT_REEL_0, placeholder("Reel 1"));
		container.setItem(SLOT_REEL_1, placeholder("Reel 2"));
		container.setItem(SLOT_REEL_2, placeholder("Reel 3"));
	}

	private ItemStack placeholder(String name) {
		ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		pane.setHoverName(Component.literal(name).withStyle(ChatFormatting.DARK_GRAY));
		addLore(pane, Component.literal("Awaiting spin...").withStyle(ChatFormatting.GRAY));
		return pane;
	}

	private ItemStack stackForRace(Race race) {
		// Deterministically map a race registry id to a visible vanilla item variant (no hard-coded per-race map)
		ResourceLocation key = TensuraRaces.RACE_REGISTRY.get().getKey(race);
		int hash = key != null ? key.toString().hashCode() : race.getNameTranslationKey().hashCode();
		int family = Math.abs(hash) % 2; // 0 = wool, 1 = stained glass pane
		family = 0;
		int colorIdx = Math.abs(hash / 2) % 16;

		ItemStack stack;
		if (family == 0) {
			// 16 wool colors
			ItemStack[] wools = new ItemStack[] {
					new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.ORANGE_WOOL),
					new ItemStack(Items.MAGENTA_WOOL), new ItemStack(Items.LIGHT_BLUE_WOOL),
					new ItemStack(Items.YELLOW_WOOL), new ItemStack(Items.LIME_WOOL),
					new ItemStack(Items.PINK_WOOL), new ItemStack(Items.GRAY_WOOL),
					new ItemStack(Items.LIGHT_GRAY_WOOL), new ItemStack(Items.CYAN_WOOL),
					new ItemStack(Items.PURPLE_WOOL), new ItemStack(Items.BLUE_WOOL),
					new ItemStack(Items.BROWN_WOOL), new ItemStack(Items.GREEN_WOOL),
					new ItemStack(Items.RED_WOOL), new ItemStack(Items.BLACK_WOOL)
			};
			stack = wools[colorIdx];
		} else {
			// 16 stained glass pane colors
			ItemStack[] panes = new ItemStack[] {
					new ItemStack(Items.WHITE_STAINED_GLASS_PANE), new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
					new ItemStack(Items.MAGENTA_STAINED_GLASS_PANE), new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE),
					new ItemStack(Items.YELLOW_STAINED_GLASS_PANE), new ItemStack(Items.LIME_STAINED_GLASS_PANE),
					new ItemStack(Items.PINK_STAINED_GLASS_PANE), new ItemStack(Items.GRAY_STAINED_GLASS_PANE),
					new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE), new ItemStack(Items.CYAN_STAINED_GLASS_PANE),
					new ItemStack(Items.PURPLE_STAINED_GLASS_PANE), new ItemStack(Items.BLUE_STAINED_GLASS_PANE),
					new ItemStack(Items.BROWN_STAINED_GLASS_PANE), new ItemStack(Items.GREEN_STAINED_GLASS_PANE),
					new ItemStack(Items.RED_STAINED_GLASS_PANE), new ItemStack(Items.BLACK_STAINED_GLASS_PANE)
			};
			stack = panes[colorIdx];
		}
		stack.setHoverName(Component.translatable(race.getNameTranslationKey()));
		setRaceLore(stack, race, false, true);
		return stack;
	}

	private Race randomRace() {
		return availableRaces.get(random.nextInt(availableRaces.size()));
	}

	private void startRolling() {
		phase = Phase.ROLLING;
		ticks = 0;
		finalRace0 = null;
		finalRace1 = null;
		finalRace2 = null;
		// staggered stop times
		stopTick0 = 40 + random.nextInt(10);
		stopTick1 = stopTick0 + 20 + random.nextInt(10);
		stopTick2 = stopTick1 + 20 + random.nextInt(10);
		// next change ticks
		nextChangeTick0 = 0;
		nextChangeTick1 = 0;
		nextChangeTick2 = 0;

		ItemStack choosing = new ItemStack(Items.RED_STAINED_GLASS_PANE);
		choosing.setHoverName(Component.literal("Rolling...").withStyle(ChatFormatting.RED));
		addLore(choosing, Component.literal("Good luck!").withStyle(ChatFormatting.GOLD));
		container.setItem(SLOT_START, choosing);
	}

	// Called server-side each server tick
	void serverTick() {
		if (phase != Phase.ROLLING) {
			return;
		}
		ticks++;
		boolean changed = false;

		// update reels at different speeds
		if (ticks >= nextChangeTick0 && ticks < stopTick0) {
			container.setItem(SLOT_REEL_0, stackForRace(randomRace()));
			nextChangeTick0 = ticks + 2;
			playDing(0, 1.2F);
			changed = true;
		}
		if (ticks >= stopTick0 && finalRace0 == null) {
			finalRace0 = randomRace();
			ItemStack s = stackForRace(finalRace0);
			setRaceLore(s, finalRace0, true, false);
			container.setItem(SLOT_REEL_0, s);
			playStop(0, 1.0F);
			changed = true;
		}

		if (ticks >= nextChangeTick1 && ticks < stopTick1) {
			container.setItem(SLOT_REEL_1, stackForRace(randomRace()));
			nextChangeTick1 = ticks + 3;
			playDing(1, 1.3F);
			changed = true;
		}
		if (ticks >= stopTick1 && finalRace1 == null) {
			finalRace1 = randomRace();
			ItemStack s = stackForRace(finalRace1);
			setRaceLore(s, finalRace1, true, false);
			container.setItem(SLOT_REEL_1, s);
			playStop(1, 0.9F);
			changed = true;
		}

		if (ticks >= nextChangeTick2 && ticks < stopTick2) {
			container.setItem(SLOT_REEL_2, stackForRace(randomRace()));
			nextChangeTick2 = ticks + 4;
			playDing(2, 1.4F);
			changed = true;
		}
		if (ticks >= stopTick2 && finalRace2 == null) {
			finalRace2 = randomRace();
			ItemStack s = stackForRace(finalRace2);
			setRaceLore(s, finalRace2, true, false);
			container.setItem(SLOT_REEL_2, s);
			playStop(2, 0.8F);
			changed = true;
		}

		// when all set, switch to choose
		if (finalRace0 != null && finalRace1 != null && finalRace2 != null) {
			phase = Phase.CHOOSE;
			ItemStack choose = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
			choose.setHoverName(Component.literal("Choose a race").withStyle(ChatFormatting.GREEN));
			addLore(choose, Component.literal("Click a reel item to select").withStyle(ChatFormatting.YELLOW));
			container.setItem(SLOT_START, choose);
			// Enhance reels for selection cue
			applyChooseCue(SLOT_REEL_0);
			applyChooseCue(SLOT_REEL_1);
			applyChooseCue(SLOT_REEL_2);
			changed = true;
		}

		// Update border animation matching progress and broadcast
		updateBorderAnimation();
		broadcastChanges();
	}

	private void applyRaceAndClose(Player player, Race race) {
		Trutils.LOGGER.info("attempting apply");
		if (!(player instanceof ServerPlayer sp)) {
			Trutils.LOGGER.info("bad player");
			return;
		}
//		try {
			Trutils.LOGGER.info("gonna try setting race");
			RaceSelectionMenu.setRace(sp, race, true, true);
			Trutils.LOGGER.info("gonna try granting resistances");
			RaceSelectionMenu.grantLearningResistance(sp);
//		} catch (Exception ignored) {
//			Trutils.LOGGER.error("something went terribly wrong: " + ignored.getMessage() + "  " + ignored.getCause());
//		}
		phase = Phase.DONE;
		sp.closeContainer();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		RaceSlotMachineTicker.untrack(this);
	}

	public Component getTitle() {
		return title;
	}

	private void playDing(int reel, float basePitch) {
		if (owner != null) {
			float jitter = (random.nextFloat() - 0.5f) * 0.2f;
			owner.playNotifySound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.5f, basePitch + jitter);
		}
	}

	private void playStop(int reel, float basePitch) {
		if (owner != null) {
			owner.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, basePitch);
		}
	}

	private void updateBorderAnimation() {
		// determine progress from rolling 0..1 based on total duration
		int total = Math.max(1, stopTick2);
		double frac = Math.min(1.0, ticks / (double) total);
		int steps = (int) Math.floor(frac * BORDER_PATH.length);

		// color completed steps with rainbow, remaining stay gray
		for (int i = 0; i < BORDER_PATH.length; i++) {
			int slot = BORDER_PATH[i];
			if (i < steps) {
				container.setItem(slot, rainbowPane(i));
			} else {
				container.setItem(slot, grayPane(" "));
			}
		}
	}

	private ItemStack grayPane(String name) {
		ItemStack pane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		pane.setHoverName(Component.literal(name));
		return pane;
	}

	private ItemStack rainbowPane(int index) {
		ItemStack[] panes = new ItemStack[] {
				new ItemStack(Items.RED_STAINED_GLASS_PANE),
				new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
				new ItemStack(Items.YELLOW_STAINED_GLASS_PANE),
				new ItemStack(Items.LIME_STAINED_GLASS_PANE),
				new ItemStack(Items.GREEN_STAINED_GLASS_PANE),
				new ItemStack(Items.CYAN_STAINED_GLASS_PANE),
				new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE),
				new ItemStack(Items.BLUE_STAINED_GLASS_PANE),
				new ItemStack(Items.PURPLE_STAINED_GLASS_PANE),
				new ItemStack(Items.MAGENTA_STAINED_GLASS_PANE),
				new ItemStack(Items.PINK_STAINED_GLASS_PANE),
				new ItemStack(Items.GRAY_STAINED_GLASS_PANE),
				new ItemStack(Items.WHITE_STAINED_GLASS_PANE),
				new ItemStack(Items.BROWN_STAINED_GLASS_PANE),
				new ItemStack(Items.BLACK_STAINED_GLASS_PANE)
		};
		ItemStack pane = panes[index % panes.length].copy();
		pane.setHoverName(Component.literal(" "));
		return pane;
	}

	private void addLore(ItemStack stack, Component... lines) {
		CompoundTag display = stack.getOrCreateTagElement("display");
		ListTag lore = new ListTag();
		for (Component c : lines) {
			lore.add(StringTag.valueOf(Component.Serializer.toJson(c)));
		}
		display.put("Lore", lore);
	}

	private void setFinalLore(ItemStack stack) {
		addLore(stack,
				Component.literal("Finalized").withStyle(ChatFormatting.GOLD),
				Component.literal("Awaiting other reels...").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
		);
	}

	private void applyChooseCue(int slot) {
		ItemStack s = container.getItem(slot).copy();
//		addLore(s, Component.literal("Click to choose").withStyle(ChatFormatting.GREEN));
		try {
			s.enchant(Enchantments.UNBREAKING, 1);
			CompoundTag tag = s.getOrCreateTag();
			tag.putInt("HideFlags", 1); // hide enchantment text, keep glint
		} catch (Exception ignored) {}
		container.setItem(slot, s);
	}

	private void setRaceLore(ItemStack stack, Race race, boolean finalized, boolean spinning) {
		ListTag lore = new ListTag();

		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.literal("⌖ Race Details ⌖").withStyle(ChatFormatting.AQUA)
			)));

		ComponentContents contents = race.getDifficulty().asText().getContents();

		if (contents instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			Style style = race.getDifficulty().asText().getStyle();
			lore.add(StringTag.valueOf(Component.Serializer.toJson(
					Component.literal("Difficulty: ").withStyle(style)
							.append(Component.translatable(key))
			)));
		}

		Pair<Double, Double> auraRange = race.getBaseAuraRange();
		Pair<Double, Double> magiculeRange = race.getBaseMagiculeRange();

		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.literal("Aura Range: ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(auraRange.getFirst().toString()).withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
						.append(Component.literal(auraRange.getSecond().toString()).withStyle(ChatFormatting.AQUA))
			)));

		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.literal("Magicules: ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(magiculeRange.getFirst().toString()).withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
						.append(Component.literal(magiculeRange.getSecond().toString()).withStyle(ChatFormatting.AQUA))
			)));

		lore.add(StringTag.valueOf(Component.Serializer.toJson(
				Component.literal("Health: ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(String.valueOf(race.getBaseHealth())).withStyle(ChatFormatting.RED))
			)));

		List<TensuraSkill> intrinsics = race.getIntrinsicSkills(owner);

		if (intrinsics.isEmpty()) {
			lore.add(StringTag.valueOf(Component.Serializer.toJson(
					Component.literal("Intrinsics: ").withStyle(ChatFormatting.GOLD)
							.append(Component.literal("None").withStyle(ChatFormatting.RED))
			)));
		} else {
			lore.add(StringTag.valueOf(Component.Serializer.toJson(
					Component.literal("Intrinsics: ").withStyle(ChatFormatting.GOLD)
			)));

			for (TensuraSkill skill : intrinsics) {

				ComponentContents skillContent = Objects.requireNonNull(skill.getColoredName()).getContents();

				if (skillContent instanceof TranslatableContents translatable) {
					String key = translatable.getKey();
					Style style = skill.getColoredName().getStyle();

					lore.add(StringTag.valueOf(Component.Serializer.toJson(
							Component.literal("  • ").withStyle(style)
									.append(Component.translatable(key).withStyle(style))
					)));
				}
			}
		}

		CompoundTag display = stack.getOrCreateTagElement("display");
		display.put("Lore", lore);
	}
}