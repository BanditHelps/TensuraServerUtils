package com.github.b4ndithelps.trutils.mixin;

import com.github.b4ndithelps.gamerules.ModGameRules;
import com.github.b4ndithelps.menus.RaceSlotMachineMenu;
import com.github.b4ndithelps.trutils.Trutils;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.menu.RaceSelectionMenu;
import com.github.manasmods.tensura.race.Race;
import com.github.manasmods.tensura.registry.race.TensuraRaces;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

import static com.github.manasmods.tensura.capability.race.TensuraPlayerCapability.getFrom;
import static com.github.manasmods.tensura.capability.race.TensuraPlayerCapability.loadRaces;

@Mixin(value = TensuraPlayerCapability.class, remap = false)
public class TensuraPlayerCapabilityMixin {

	@Inject(
		method = "checkForFirstLogin(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void trutils$maybeEarlyExitFirstLogin(Player entity, CallbackInfo ci) {
		if (entity instanceof ServerPlayer player && player.getServer() != null) {
			boolean forceRandom = player.getServer().getGameRules().getBoolean(ModGameRules.FORCE_RANDOM_RACE);
			boolean gachaMode = player.getServer().getGameRules().getBoolean(ModGameRules.GACHA_MODE);
			if (forceRandom) {
				getFrom(player).ifPresent((cap) -> {
					if (cap.getRace() == null) {
						Trutils.LOGGER.info("TRUtils: Skipping Tensura first login flow for {}", player.getScoreboardName());

						// If gacha mode is enabled, show the slot machine interface
						if (gachaMode) {
							try {
								MenuProvider provider = new MenuProvider() {
									@Override
									public Component getDisplayName() {
										return Component.literal("Race Selection");
									}

									@Override
									public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
										return new RaceSlotMachineMenu(id, playerInventory, new SimpleContainer(RaceSlotMachineMenu.SIZE), getDisplayName(), player);
									}
								};
								player.openMenu(provider);
							} catch (Exception e) {
								Trutils.LOGGER.error("Error in showing player the gacha screen!" + e.getMessage());
							}
						} else {
							// Gets all the random races in the config
							List<ResourceLocation> randomRaceList = loadRaces();

							// Random selection
							Random random = new Random();
							int randomIndex = random.nextInt(randomRaceList.size());

							Race selectedRace = TensuraRaces.RACE_REGISTRY.get().getValue(randomRaceList.get(randomIndex));

							try {
								RaceSelectionMenu.setRace(player, selectedRace, true, true);
								RaceSelectionMenu.grantLearningResistance(player);
							} catch (Exception e) {
								Trutils.LOGGER.error("Error in selecting random race: " + randomRaceList.get(randomIndex));
							}
						}
					}
				});

				ci.cancel();
			} else {
				Trutils.LOGGER.info("TRUtils: No Skip needed. Force Gamerule is false");
			}
		}
	}

}


