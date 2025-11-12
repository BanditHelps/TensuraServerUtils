package com.github.b4ndithelps.trutils;

import com.github.b4ndithelps.gamerules.ModGameRules;
import com.github.b4ndithelps.menus.LeaderboardScreen;
import com.github.b4ndithelps.menus.ModMenus;
import com.github.b4ndithelps.commands.LeaderboardCache;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@SuppressWarnings("removal")
// The value here should match an entry in the META-INF/mods.toml file
@Mod(Trutils.MODID)
public class Trutils {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "trutils";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

	// GameRule to control skipping Tensura's first login flow
	public static GameRules.Key<GameRules.BooleanValue> DISABLE_TENSURA_FIRST_LOGIN;

    public Trutils() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        ModMenus.register(modEventBus);



        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                ModGameRules.registerGameRules());

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("TensuraServerUtils Starting...");
		LeaderboardCache.loadFromDisk(event.getServer());
		LeaderboardCache.refresh(event.getServer());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.LEADERBOARD_MENU.get(), LeaderboardScreen::new);
            });
        }
    }
}