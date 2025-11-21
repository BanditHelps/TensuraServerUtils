package com.github.b4ndithelps.trutils.mixin;

import com.github.b4ndithelps.gamerules.ModGameRules;
import com.github.b4ndithelps.menus.RaceSlotMachineMenu;
import com.github.b4ndithelps.trutils.Trutils;
import com.github.manasmods.manascore.api.skills.ManasSkill;
import com.github.manasmods.manascore.api.skills.ManasSkillInstance;
import com.github.manasmods.manascore.api.skills.SkillAPI;
import com.github.manasmods.manascore.api.skills.capability.SkillStorage;
import com.github.manasmods.manascore.api.skills.event.RemoveSkillEvent;
import com.github.manasmods.tensura.ability.SkillUtils;
import com.github.manasmods.tensura.ability.TensuraSkillInstance;
import com.github.manasmods.tensura.ability.magic.Magic;
import com.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import com.github.manasmods.tensura.ability.skill.unique.CookSkill;
import com.github.manasmods.tensura.capability.effects.TensuraEffectsCapability;
import com.github.manasmods.tensura.capability.ep.TensuraEPCapability;
import com.github.manasmods.tensura.capability.race.TensuraPlayerCapability;
import com.github.manasmods.tensura.capability.skill.TensuraSkillCapability;
import com.github.manasmods.tensura.item.custom.ResetScrollItem;
import com.github.manasmods.tensura.menu.RaceSelectionMenu;
import com.github.manasmods.tensura.registry.skill.UniqueSkills;
import com.github.manasmods.tensura.world.TensuraGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

import static com.github.manasmods.tensura.item.custom.ResetScrollItem.isIntrinsicSkills;
import static com.github.manasmods.tensura.item.custom.ResetScrollItem.resetFlight;

@Mixin(value = ResetScrollItem.class, remap = false)
public abstract class ResetScrollItemMixin {

    @Shadow
    public static void resetRaceFailsafe(Player player) {
    }

    @Inject(
            method = "resetRace",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/manasmods/tensura/item/custom/ResetScrollItem;resetRaceFailsafe(Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true
    )
    private static void trutils$gachaRaceOverride(ServerPlayer player, CallbackInfo ci) {
        Trutils.LOGGER.info("Injected Success");

        boolean gachaMode = player.getServer().getGameRules().getBoolean(ModGameRules.GACHA_MODE);

        if (gachaMode) {
            // This is the default code from the Tensura: Reincarnated mod. The only difference
            // is that it will call the gacha screen

            resetRaceFailsafe(player);
            TensuraPlayerCapability.getFrom(player).ifPresent((cap) -> {
                if (cap.getRace() != null) {
                    SkillStorage storage = SkillAPI.getSkillsFrom(player);
                    Iterator<ManasSkillInstance> iterator = storage.getLearnedSkills().iterator();

                    label37:
                    while(true) {
                        TensuraSkillInstance instance;
                        do {
                            Object patt12668$temp;
                            do {
                                if (!iterator.hasNext()) {
                                    storage.syncAll();
                                    break label37;
                                }

                                patt12668$temp = iterator.next();
                            } while(!(patt12668$temp instanceof TensuraSkillInstance));

                            instance = (TensuraSkillInstance)patt12668$temp;
                        } while(!isIntrinsicSkills(player, cap, cap.getRace(), instance) && !(instance.getSkill() instanceof Magic) && !(instance.getSkill() instanceof ResistSkill));

                        if (!MinecraftForge.EVENT_BUS.post(new RemoveSkillEvent(instance, player))) {
                            iterator.remove();
                        }
                    }
                }

                cap.clearIntrinsicSkills();
                TensuraPlayerCapability.resetEverything(player);
                if (SkillUtils.hasSkill(player, (ManasSkill) UniqueSkills.CHOSEN_ONE.get())) {
                    cap.setBlessed(true);
                    TensuraPlayerCapability.sync(player);
                }

            });
            CookSkill.removeCookedHP(player, (ManasSkillInstance)null);
            TensuraEPCapability.resetEverything(player);
            TensuraSkillCapability.resetEverything(player, false, true);
            TensuraEffectsCapability.resetEverything(player, true, true);
            player.setRespawnPosition(Level.OVERWORLD, (BlockPos)null, 0.0F, false, false);

            try {
                MenuProvider provider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Race Selection");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
                        return new RaceSlotMachineMenu(id, playerInventory, new SimpleContainer(RaceSlotMachineMenu.SIZE), getDisplayName(), player, false);
                    }
                };
                player.setInvulnerable(true);
                player.openMenu(provider);
            } catch (Exception e) {
                Trutils.LOGGER.error("Error in showing player the gacha screen!" + e.getMessage());
                player.setInvulnerable(false);
            }

            resetFlight(player);
            ci.cancel();
        }
    }

    @Inject(
            method = "resetEverything",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/manasmods/tensura/item/custom/ResetScrollItem;resetRaceFailsafe(Lnet/minecraft/world/entity/player/Player;)V"
            ),
            cancellable = true
    )
    private static void trutils$gachaFullResetOverride(ServerPlayer player, CallbackInfo ci) {
        boolean gachaMode = player.getServer().getGameRules().getBoolean(ModGameRules.GACHA_MODE);

        if (gachaMode) {
            resetRaceFailsafe(player);
            if (player.getLevel().getGameRules().getBoolean(TensuraGameRules.RIMURU_MODE)) {
                RaceSelectionMenu.reincarnateAsRimuru(player);
            } else {
                player.setInvulnerable(true);
                try {
                    MenuProvider provider = new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.literal("Race Selection");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
                            return new RaceSlotMachineMenu(id, playerInventory, new SimpleContainer(RaceSlotMachineMenu.SIZE), getDisplayName(), player, true);
                        }
                    };

                    player.openMenu(provider);
                } catch (Exception e) {
                    Trutils.LOGGER.error("Error in showing player the gacha screen!" + e.getMessage());
                    player.setInvulnerable(false);
                }
            }

            resetFlight(player);
            ci.cancel();
        }
    }
}
