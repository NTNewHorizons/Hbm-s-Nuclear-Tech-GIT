# Changes Compared to Upstream
**New Content**
- Added the Chungus Solar Array, a large, rotatable solar generator with improved efficiency.
- Added the Railgun Turret, including its ammunition and projectiles.
- Added the Conveyor Combinator.
- Added the Fluid Regulator Valve for configurable underflow and overflow control.
- Restored the Radial Performance Engine (Selenium Engine).
- Added new world structures, including camps and an airplane.
- Added custom bobbleheads, plushies, and decorative items, including Bufka's head.
- Added an OpenComputers-themed case block.
- Added additional cosmetic equipment.
- Added the combustible lemon.
- Added Glyphid taming, dancing, and maracas.
- Added custom space-themed music, Bobmazon background music, and new music discs.
- Added the can crate to the default Bobmazon item pool.

**World Generation and Progression**
- Added depletable rich ore deposits with configurable richness, biome-based generation, and automated mining support.
- Replaced native strawberries with HarvestCraft strawberries.
- Made dead grass recovery ten times slower.
- Added custom recipes and progression changes for NT:NH.
- Added a lodestone recipe and coal processing in the Combination Oven.
- Added Ore Dictionary support for bedrock ores.
- Adapted Meteorite Sword breeding to use the Chicago Pile.

**Machines and Automation**
- Added fractional AUTOCAL clock speeds, allowing scripts to run slower than one line per tick.
- Improved conveyors with lane preservation, balanced splitting, better routing, and safer overflow handling.
- Added configurable energy buffers to HE/RF converters.
- Improved machine upgrade handling with slot validation and detailed upgrade effect information.
- Extended the Fluid Siphon to support partially filled fluid barrels.
- Expanded custom-machine configuration with molten-material processing, custom OBJ models, and operation sounds.

**Mod Compatibility**
- Added Forge Fluids integration, allowing NTM fluid systems to interact with other mods, including Applied Energistics 2.
- Added Baubles, Baubles Expanded, and Backhand compatibility for compatible equipment, chargers, and refuelers.
- Added AgriCraft compatibility to the Automatic Thresher and Buzz Saw.
- Expanded OpenComputers compatibility with additional recipes and component support.
- Expanded Redstone over Radio with crate and Storage Drawers reading, custom item-name signals, and programmable fluid identification.
- Added Special Mobs compatibility for creeper transformations.

**Gameplay and Quality of Life**
- Enabled Nether heat and burning mechanics, with improved armor protection information.
- Adjusted Reacher radiation to use inverse-square distance attenuation.
- Removed misleading cosmetic burning effects.
- Made upright steel scaffolding climbable.
- Added graphical fuel information to RBMK and PWR rod tooltips.
- Remodeled and retextured the Electric Press.
- Expanded internationalization, replacing hardcoded text with translation keys and improving localization support.

**Performance**
- Optimized entity and machine chunkloading to reduce unnecessary chunk operations.
- Optimized machine redstone handling to avoid unnecessary polling.

**Development**
- Migrated to GTNH Gradle with JDK 25 and JVM Downgrader, while maintaining Java 8 runtime compatibility.
- Improved GitHub Actions builds and release artifact publishing.
- Added automated line-ending normalization and validation.
- Added multiblock machine documentation.
