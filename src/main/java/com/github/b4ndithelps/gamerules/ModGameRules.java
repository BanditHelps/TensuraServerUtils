package com.github.b4ndithelps.gamerules;

import com.github.b4ndithelps.trutils.Trutils;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;


public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> FORCE_RANDOM_RACE;
    public static GameRules.Key<GameRules.BooleanValue> GACHA_MODE;

    public ModGameRules() {

    }

    public static void registerGameRules() {
        FORCE_RANDOM_RACE = GameRules.register("forceRandomRace", GameRules.Category.MISC, create(false));
        Trutils.LOGGER.info("Successfully registered forceRandomRace GameRule.");
        GACHA_MODE = GameRules.register("gachaMode", GameRules.Category.MISC, create(false));
        Trutils.LOGGER.info("Successfully registered gachaMode GameRule.");
    }

    @SuppressWarnings("unchecked")
    public static GameRules.Type<GameRules.BooleanValue> create(boolean defaultValue) {
        try {
            Method createGameruleMethod = ObfuscationReflectionHelper.findMethod(GameRules.BooleanValue.class, "m_46250_", boolean.class);
            createGameruleMethod.setAccessible(true);
            return (GameRules.Type<GameRules.BooleanValue>) createGameruleMethod.invoke(GameRules.BooleanValue.class, defaultValue);
        }
        catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}
