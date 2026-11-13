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
- Remodeled and retextured the Electric Press.

**World Generation and Progression**
- Added depletable rich ore deposits with configurable richness, biome-based generation, and automated mining support.
- Replaced native strawberries with HarvestCraft strawberries.
- Made dead grass recovery ten times slower.
- Added custom recipes and progression changes for NT:NH.
- Added a lodestone recipe and coal processing in the Combination Oven.
- Added Ore Dictionary support for bedrock ores.
- Adapted Meteorite Sword breeding to use the Chicago Pile.

**Machines and Automation**
- Added Item Export Upgrade which exports machine outputs into adjacent inventories automatically.
- Added decimal AUTOCAL clockspeeds, allowing scripts to run slower than one line per tick.
- Added AUTOCAL stepping for line-by-line debugging, with a pause indicator and Shift-click to resume execution.
- Fixed AUTOCAL script opening on some computers.
- Improved conveyors with lane preservation, balanced splitting, better routing, and safer overflow handling.
- Added GUIs for HE/RF converters with configurable energy buffers and HE input priority.
- Improved machine upgrade handling with slot validation and detailed upgrade effect information.
- Extended the Fluid Siphon to support partially filled fluid barrels.
- Expanded custom-machine configuration with molten-material processing, custom OBJ models, and operation sounds.
- Extended ROR with crate item info and extra fluid tank info.
- Added ROR `setfluid` support for some machines.

**Mod Compatibility**
- Added Forge Fluids integration, allowing NTM fluid systems to interact with other mods, including Applied Energistics 2.
- Added Baubles, Baubles Expanded, and Backhand compatibility for compatible equipment, chargers, and refuelers.
- Added AgriCraft compatibility to the Automatic Thresher and Buzz Saw.
- Expanded OpenComputers compatibility with additional recipes and component support.
- Expanded ROR with Storage Drawers item info.
- Added Special Mobs compatibility for creeper transformations.

**Gameplay and Quality of Life**
- Added a tool modifier to add a configurable mining delay to instamine tools.
- Enabled Nether heat and burning mechanics, with improved armor protection information.
- Adjusted Reacher radiation to use inverse-square distance attenuation.
- Removed misleading cosmetic burning effects.
- Made upright steel scaffolding climbable.
- Added graphical fuel information to RBMK and PWR rod tooltips.
- Expanded internationalization, replacing hardcoded text with translation keys and improving localization support.

**Performance**
- Optimized entity and machine chunkloading to reduce unnecessary chunk operations.
- Optimized machine redstone handling to avoid unnecessary polling.

**Development**
- Migrated to GTNH Gradle, using the JDK 25 compiler while maintaining Java 8 runtime compatibility.
- Improved development build performance with Gradle build caching, a separate mixin source set, and uncompressed development JARs.
- Improved GitHub Actions builds and release artifact publishing.
- Added automated publishing to the NTNH Maven repository.
- Added automated line-ending normalization and validation.
- Added multiblock machine documentation.
