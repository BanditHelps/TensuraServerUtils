package com.github.b4ndithelps.menus;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Trutils.MODID)
public class RaceSlotMachineTicker {
	private static final Set<RaceSlotMachineMenu> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

	static void track(RaceSlotMachineMenu menu) {
		ACTIVE.add(menu);
	}

	static void untrack(RaceSlotMachineMenu menu) {
		ACTIVE.remove(menu);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (ACTIVE.isEmpty()) return;
		for (RaceSlotMachineMenu menu : ACTIVE.toArray(new RaceSlotMachineMenu[0])) {
			if (menu != null) {
				menu.serverTick();
			}
		}
	}
}


