package com.github.b4ndithelps.gamerules;

import net.minecraft.world.level.GameRules;

import java.lang.reflect.Method;


public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> FORCE_RANDOM_RACE;

    public ModGameRules() {

    }

    public static void registerGameRules() {
        try {
            Method createMethod = GameRules.BooleanValue.class.getDeclaredMethod("create", boolean.class);
            createMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            GameRules.Type<GameRules.BooleanValue> type =
                    (GameRules.Type<GameRules.BooleanValue>) createMethod.invoke(null, false);

            FORCE_RANDOM_RACE = GameRules.register("forceRandomRace", GameRules.Category.PLAYER, type);

        } catch (Exception e) {
            throw new RuntimeException("Failed to register gamerule forceRandomRace", e);
        }
    }

//    public static void registerGameRules() {
//        FORCE_RANDOM_RACE = GameRules.register("forceRandomRace", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
//    }
}
