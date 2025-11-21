# Tensura: Server Utils

Tensura: Server Utils is an add-on for Tensura: Reincarnated that adds in a few features geared to adding more fun and features to servers or modpacks using the main mod.

## Features
## Game Rules
forceRandomRace - Removes the default race selection screen, and automatically chooses a random race of the available options. Only affects the first time login. (default: false)

gachaMode - Replaces the default race selection screen with the gacha machine. Only works if the forceRandomRace game rule is true. (default: false) 



## Gacha Mode

Gacha Mode is a fun way to randomize player's starting races, while still providing a limited choice. Upon clicking the lever in the new Race Selection screen, a roll animation will play. Then, player's have the option of 3 completely random starting races to choose from. All races configured in the /defaultconfig/ directory are included in the roll.
![racagacha-ezgif com-optimize](https://github.com/user-attachments/assets/8f767503-7037-4f82-a96c-a7b1c39464d2)


## Global Leaderboard

Using the /leaderboard command, all players are able to view the EP leaderboard. Every player, both online and offline, will be listed on this leaderboard. The leaderboard refreshes every 30 seconds. Players are ordered by EP, and it also lists their race and number of unique skills. To add "fake" players to the leaderboard, use the command /leaderboard test add <name> <ep> To remove "fake" players from the leaderboard, use /leaderboard test remove <name>

![leaderboard-ezgif com-optimize](https://github.com/user-attachments/assets/caebb5ca-fc7f-447d-9ceb-4bcd61f08b01)


## Skill Disable via Config

An excellent feature for server owners and modpack developers. Allows for the prevention of any skill from being learned by a player. Especially useful for addons that may introduce broken or buggy skills.

To prevent a player from learning any skill, simply open the /config/trutils-common.toml file. Then, add the id of the skill that you want to prevent to the array of values.

For Example, the following will prevent Predator from base Tensura, and the Plunderer skill from Mysticism:
blockedSkills = ["tensura:predator", "trmysticism:plunderer"]

This will work with all skill types: Intrinsic, Common, Extra, Unique, and Ultimate.

Hopefully you enjoy!
