# Minion's Workshop

Craft, smelt, and store items in one modular workstation.

Minion's Workshop is an unofficial modern port of **Engineer's Workshop Reborn**, continuing the original **Steve's Workshop** concept. Supports **Minecraft 1.21.1, 26.1.2, and 26.3** on **Fabric and NeoForge**.

## Features

- **Four station tabs** - Install crafting tables, furnaces, or chests to build the workstation you need. Each tab opens the installed station's GUI; right-click its icon to remove the block without deleting its stored items.
- **Expandable side tabs** - Icon-based upgrade, transfer, and item-filter panels expand when clicked and collapse when clicked again. Their controls follow the selected station, while global upgrades remain available on every tab.
- **Automatic crafting** - Use the Auto Crafter upgrade to craft recipes automatically while reserving one item in each occupied pattern slot.
- **Expandable storage** - Chest modules provide 24 storage slots each. The Storage upgrade adds six ingredient slots to crafting modules.
- **Shared power** - Solid fuel, external energy, and liquid fuel power the same crafting, smelting, and charging buffer, holding up to 64,000 FE-equivalent energy. Its bar stays visible beside your inventory on every station tab.
- **Energy integration** - The Energy Input upgrade accepts Forge Energy on NeoForge and TechReborn Energy on Fabric, plus optional Modern Industrialization and Singularity Iteration EU on NeoForge.
- **Liquid fuel** - The Liquid Fuel upgrade adds an 8,000 mB lava tank. Fill or drain it through the lava-bucket control beside the shared power bar, or connect compatible fluid pipes.
- **Machine upgrades** - Increase production speed, queue furnace inputs, or use Chargers to store operations for instant production.
- **Item automation** - Configure input and output independently for each module and side. Enable automatic transfers to adjacent inventories and upgrade their transfer capacity.
- **Item filters** - A dedicated panel lays out Input and Output filters together. Use ghost filters with exact data-component, item-identity, fuzzy, or shared-tag matching.
- **JEI support** - JEI reserves space around the animated side tabs so its ingredient list does not cover the workshop's controls.
- **Persistent configuration** - Inventories, machine progress, stored charges, exact shared power, lava, filters, and transfer settings survive world reloads. Changing a module preserves its inactive inventory; removing a power upgrade preserves stored resources.

## Getting started

Craft a Workshop Table from a crafting table, two iron ingots, and six planks. Open it and place a crafting table, furnace, or chest into an empty top tab. Add station-specific or global upgrades through the expanding **Upgrades** side tab.

Install **Energy Input** to use energy cables, or **Liquid Fuel** to use lava buckets and fluid pipes. Enable the required faces in the **shared-fuel transfers** side tab: Input receives energy or lava, and Output allows lava withdrawal. Auto Transfer is only needed for the workshop's automatic item transfers, not energy cables or fluid pipes.

With Liquid Fuel installed, lava buckets in the fuel slot also fill the tank and return empty buckets. Click the lava control with a lava bucket to deposit 1,000 mB, or an empty bucket to withdraw it. Exchanges are all-or-nothing; a stack of empty buckets needs free player-inventory space for the filled bucket.

## Requirements

[![Requires on Fabric: Fabric API](https://img.shields.io/badge/Requires_on_Fabric-Fabric_API-DBD0B4?style=for-the-badge&logo=fabric&logoColor=black)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

- **Minecraft:** 1.21.1, 26.1.2, or 26.3.
- **Java:** 21 for Minecraft 1.21.1; 25 for Minecraft 26.1.2 and 26.3.
- **Fabric:** Fabric Loader and Fabric API.
- **NeoForge:** NeoForge for the matching Minecraft version. No additional library mods are required.

Install the file matching your Minecraft version and mod loader. Fabric API is only required for the Fabric version.

## Compatibility

Hoppers and compatible item pipes can access enabled input and output sides. No optional integration mods are required.

[![Forge Energy](https://img.shields.io/badge/Integration-Forge_Energy-AF6F3F?style=for-the-badge)](https://neoforged.net/)
[![TechReborn Energy API](https://img.shields.io/badge/Integration-TechReborn_Energy_API-E04949?style=for-the-badge)](https://github.com/TechReborn/Energy)
[![Modern Industrialization](https://img.shields.io/badge/Integration-Modern_Industrialization-587E9F?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/modern-industrialization)
[![Singularity Iteration](https://img.shields.io/badge/Integration-Singularity_Iteration-6B669B?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/singularity-iteration)
[![Optional: JEI](https://img.shields.io/badge/Optional-JEI-7E7E7E?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/jei)

| Integration | Loader | What to install |
| --- | --- | --- |
| Forge Energy | NeoForge | Energy Input upgrade; no additional energy API mod |
| TechReborn Energy API | Fabric | Energy Input upgrade; the standalone API is bundled |
| Modern Industrialization MI EU | NeoForge | Energy Input upgrade and a compatible MI build |
| Singularity Iteration EU | NeoForge | Energy Input upgrade and a compatible SI build |
| Lava buckets and fluid pipes | Fabric and NeoForge | Liquid Fuel upgrade |
| JEI | Fabric and NeoForge | Optional, matching the Minecraft version and loader |

Eight FE or TechReborn energy equals one fuel unit. Energy intake has no workshop-imposed per-tick limit beyond the buffer's free space, and the workshop does not export energy. The full TechReborn mod is not required.

Industrial EU uses the supplying mod's conversion ratio: MI follows its configurable FE-per-EU setting, while SI uses its native EU conversion. The workshop connects directly to SI's native capability instead of relying on SI's MI compatibility bridge.

Modern Industrialization and Singularity Iteration are optional. Their published compatible builds used for in-game verification are for **1.21.1 NeoForge**; no published 26.x build was available for verification. These NeoForge-only APIs are not loaded on Fabric.

The tank accepts lava fuel only, not water or general-purpose fluids. One lava bucket provides 20,000 fuel units, converted into the shared buffer as space becomes available. Stored lava remains available after reinstalling Liquid Fuel. Tesla and arbitrary modded station installation are not supported.

**Existing worlds:** This port uses the new `minions_workshop` namespace and does not automatically convert blocks from older Engineer's Workshop versions. Back up your world before switching mods.

## Credits

This is an unofficial port maintained by **Minionade**, based on **Engineer's Workshop Reborn** by **Modmuss50, EwyBoy, JorVa, Vswe, and contributors**. The original **Steve's Workshop** concept is credited to **Vswe**.

Original code and texture credits belong to their respective creators. This project is not an official release of or affiliated with Engineer's Workshop Reborn.

Licensed under the **MIT License**.

[View the source code](https://github.com/Minionade/minions-workshop) | [Report an issue](https://github.com/Minionade/minions-workshop/issues) | [Original CurseForge project](https://www.curseforge.com/minecraft/mc-mods/engineers-workshop-reborn)
