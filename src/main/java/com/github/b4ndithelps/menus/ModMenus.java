package com.github.b4ndithelps.menus;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Trutils.MODID);

    public static final RegistryObject<MenuType<LeaderboardMenu>> LEADERBOARD_MENU = MENUS.register("leaderboard_menu",
            () -> IForgeMenuType.create((id, inv, buf) -> new LeaderboardMenu(id, inv)));

    public static final RegistryObject<MenuType<RaceSlotMachineMenu>> RACE_SLOT_MACHINE_MENU = MENUS.register("race_slot_machine_menu",
            () -> IForgeMenuType.create((id, inv, buf) -> new RaceSlotMachineMenu(id, inv)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
