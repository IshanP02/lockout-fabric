## Lockout v0.10.9
- Changed how Pick/Bans work. Now there are designated BAN rounds and PICK rounds
## Lockout v0.10.8
- Added autocomplete for lockout commands
- Various Visual Improvements
- Various Bugfixes
## Lockout v0.10.7
- Added New Goals:
	- Eat All Soups
	- Have Most Creeper Kills (KOTH)
	- Damaged By 12 Unique Sources
	- Obtain 3 Unique Music Discs
	- Obtain Dried Kelp Block
- Updated Old Goals with 1.21.8 Items
	- Obtain 4 Unique Saplings (Pale Oak Sapling)
	- Obtain 6 Unique Flowers (Cactus Flower, Wildflowers, Pink Petals, Open/Closed Eyeblossom)
## Lockout v0.10.6
- Added New Goals:
	- Lock Map using Cartography Table
	- Leash 4/6/8 Unique Entities at Once
	- Attach Lead to Cow
	- Attach Lead to Cherry Chest Boat
	- Attach Lead to Dolphin
	- Attach Lead to Frog
	- Attach Lead to Fox
	- Attach Lead to Iron Golem
	- Attach Lead to Strider
## Lockout v0.10.5
- Added Goal Pings
## Lockout v0.10.4
- Merged BoardType with Pick Ban GUI
- Added /SimulatePickBans
	- Simulates a Turn-Based Pick/Ban system that goes through Rounds of Picks and Bans
	- Automatically filters out goals excluded in /BoardType
- Added /MaxRounds to add how many Rounds a PickBanSession has
- Added /PickBanSelectionLimit to limit how many goals can be picked/banned per round.
- Added /CancelPickBanSession to force stop a PickBanSession
- Fixed BoardType to work on servers
- Fixed various bugs related to PickBanSessions and BoardTypes
- Fixed autocomplete in lockout and blackout (Taken from Specnr)
- Added random team option to lockout
## Lockout v0.10.3
- Added New Commands
	- Added CreateBoardGroup command
	- Added DeleteBoardGroup command
	- Added ListBoardGroups command
	- Added EditBoardGroup command
	- Changed board limit from 7 to 12 and lower limit from 3 to 1
## Lockout v0.10.2
- Added Goals:
	- Die to Pufferfish
	- Kill Blaze with Snowball (Taken from Specnr)
	- Have the Most Advancements (Taken from Specnr)
	- Have Effects Applied for 5 Minutes
	- Have Effects Applied for 8 Minutes
	- Have Effects Applied for 10 Minutes
- Added tooltip functionality to Visit All Nether Biomes Goal (so it shows the player which biomes they've visited)
- Changed how Time Based Goals are Calculated for Team Goals:
	- Time is now additive between all team members (If 2 people are doing the time goal, the time ticks at twice the rate)
	- Tooltip now updates for all players on the team regardless of who is doing the goal.
## Lockout v0.10.1
- Added Goals:
	- Added Wear 4 Different Armor Pieces Goal
## Lockout v0.10.0
- Added new keybind "P" to access the Pick/Ban GUI (only accessable to players with operator permissions)
	- Left Click on a goal to add it to your Picks
	- Right Click on a goal to add it to your Bans
- Goal Search in Pick/Ban GUI automatically filters out any goals that fail the biome/structure check.
- Added `/RemovePicks` command.
- Added `/RemoveBans` command.
- Added `/PickBanLimit <number>` command. This allows you to set a limit on the amount of picks and bans.
## Lockout v0.9.9
- Added Goals:
	- Obtain 64 Coloured Terracotta
	- Obtain Every Furnace Type
	- Obtain 5 Stone Types
## Lockout v0.9.8
- Added Goals:
	- Crouch 100m
	- Swim 500m
	- Die by Drowning
	- Obtain Dead Bush
	- Visit 10, 15, 20 Unique Biomes
	- Unlock a Vault with Trial Key
	- Find a Trial Chamber
	- Obtain Tinted Glass
	- Obtain Pottery Sherd
	- Opponent hit by Arrow
	- Take Damage from 8 Unique Sources
	- Brew a Potion of Fire Resistance
	- Obtain 3 Unique Banner Patterns
	- Obtain Every Type of Torch
	- Tune a Note Block
	- Obtain Lodestone
	- Place a Dried Ghast in Water
	- Breed Armadillos
	- Obtain 64 Coarse Dirt
	- Place a Painting
	- Obtain Calibrated Sculk Sensor
	- Obtain Smooth Quartz Stairs
	- Obtain 64 Firefly Bushes
	- Obtain Block of Resin
	- Right Click Banner with a Map
	- Obtain Sea Lantern
	- Boat 2km
	- Equip Horse with Unique Colored Leather Horse Armor
	- Eat Beetroot Soup
	- Kill Slime
	- Break any Tool
	- Break any Armor
	- Obtain Colored Harness
	- Kill Bogged
	- Obtain 64 Glow Lichen
	- Get Infested
	- Obtain 5 Unique Pressure Plates
## Lockout v0.9.7
- Added Die to Warden Goal
## Lockout v0.9.6
- Added Obtain Block of Diamond Goal
- Added Obtain Block of Emerald Goal
- Added Obtain Block of Amethyst Goal
## Lockout v0.9.5
- Added Obtain 64 Stained Glass Goals
## Lockout v0.9.4
- Completed goals in the goal menu display the player name of the player who completed the goal
  - Goals with team progression (eat unique foods or breed unique animals) will show player name.
## Lockout v0.9.3
- Fixed spectators getting advancements causing a crash

## Lockout v0.9.2
- Added `/SetBoardSize <board size>` command. This replaces `lockoutBoardSize` gamerule.
- Added `/BoardSide <left/right>` command.
- Added a config file in `.minecraft/config/lockout.json`.
    - 'default board size' - changes the board size when the server (or singleplayer world) starts.
        - default: 5
    - 'board position' - changes the position of the board (left/right)
        - default: "right"
    - 'show NoiseRouter line' - shows the long NoiseRouter line on debug hud (F3)
        - default: false
- Biome/Structure search radius has been reduced to 750 blocks (in each direction from world spawn; 1500x1500 square).
- Board is rendered above visual effects such as vignette, nausea etc.
- Fixed 'Decorate Shield with Banner'.
- Added height map debug line to debug hud (F3) - O value is often used for knowing terrain height when underground.
- Added goals:
    - 'Obtain 64 Arrows'
- Removed goals from random goal pool:
    - 'Fill Bundle with 16 empty Bundles' - overcooked

## Lockout v0.9.1
- Fixed #22 (zombie-type mobs killing anything in the nether caused a crash)

## Lockout v0.9.0
- The mod is now on version 1.21.3.
- There is a new gamerule (`lockoutBoardSize`) that allows you to play boards of different sizes. You can play boards as small as 3x3, and as large as 7x7. This change also applies to Board Builder. (#10 by @bbb651)
- Fixed most crashes and disconnect bugs.
- Biome/Structure search radius has been reduced to 1000 blocks.
- Status effects are now shown next to the board.
- Board Builder can be opened with existing boards. (`/BoardBuilder <board name>`)
- Updated some icons to better reflect their goals.
- Added 7 goals:
    - Shoot Firework from Crossbow
    - Mine Crafter
    - Light Candle
    - Wear Full Enchanted Armor
    - Put Wolf Armor on Wolf
    - Kill Breeze using Wind Charge
    - Fill Bundle with 16 empty Bundles
- Changed some goals:
    - Obtain End Crystal -> Place End Crystal
    - 'Opponent dies 3 times' is now shared in the team
    - 'Wear a carved pumpkin for 5 minutes' - progress doesn't reset after you stop wearing a pumpkin, it's saved and shown on the board
- Removed some goals:
    - Kill Ender Dragon (there is already 'Obtain Dragon Egg' goal which is more straightforward)
- Some goals are no longer part of the random goal pool, but they can still be played on custom boards:
    - End city related goals - too much rng, cities can spawn thousands of blocks away
    - Opponent obtains seeds/obsidian - no way to force these if it's left as a tie breaker
---
## Lockout v0.8.1
- Literally nothing, I released v0.8.0 again instead of v0.8.1
    - Should have implemented a fix for #6 (rejoining the server wouldn't work)

## Lockout v0.8
- Added 3 new goals:
    - 'Drink water bottle'
    - 'Use brush on suspicious sand/gravel'
    - 'Die to Polar Bear'
- Some 1v1 team goals can appear on random boards now
- Spectator advancements (e.g. entering nether/fort/bastion) will no longer display in chat
---
## Lockout v0.7:
- Added 3 new goals:
    - 'Kill bat',
    - 'Fill chiseled bookshelf'
    - 'Opponent eats food'
- Timer is now server-sided; added pre-game countdown
- Added `/SetStartTime <seconds>` command (in seconds, between 5-300)
- Changed a few goal icons
- Fixed a bug with Piglin bartering
---
## Lockout v0.6:
- Added ability to create and play custom boards (`/BoardBuilder`, `/SetCustomBoard <board name>`)
- Added spectators (every player who isn't a part of a game is a spectator)
- Villagers now always convert to Zombie Villagers if they are killed by Zombies
---
## Lockout v0.5:
- Reverted to version 1.20.1
- Team chat now works outside the game as well.
---
## Lockout v0.4:
- Added support for single-player and LAN worlds.
- Added Blackout mode (`/blackout players <player name> <player name>...` or `/blackout team <team name>`)
---
## Lockout v0.3:
- Added `/GiveGoal <player> <goal number>` command
- Added 4 new goals
- Fixed some bugs
---
## Lockout v0.2:
- Fixed many bugs, added compasses
---
## Lockout v0.1
- Initial release