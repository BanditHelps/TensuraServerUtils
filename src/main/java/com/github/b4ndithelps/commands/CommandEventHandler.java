package com.github.b4ndithelps.commands;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Trutils.MODID)
public class CommandEventHandler {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LeaderboardCommand.register(event.getDispatcher());
		RaceGachaCommand.register(event.getDispatcher());
    }
}
