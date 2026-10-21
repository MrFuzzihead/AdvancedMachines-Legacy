# IC2 AdvancedMachines Legacy

[![Build status](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/AdvancedMachines-Legacy?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![IC2](https://img.shields.io/badge/IC2-2.2.828--experimental-blue)](https://www.curseforge.com/minecraft/mc-mods/industrial-craft)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**IC2 AdvancedMachines** is an [IndustrialCraft²](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) addon for
**Minecraft 1.7.10 (Forge)** that adds five upgraded versions of the standard IC2 processing machines.
This repository is a community-maintained continuation of [AtomicStryker's original Advanced Machines](https://github.com/Jorch72/atomicstrykers-minecraft-mods)
mod, keeping the machines alive with bug fixes, IC2-parity work, and a modernized build toolchain.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Downloads](#downloads)
- [Configuration](#configuration)
- [Building from source](#building-from-source)
- [Contributing](#contributing)
- [Changes in this fork](#changes-in-this-fork)
- [Credits](#credits)
- [License](#license)

## Features

- **Rotary Macerator** — a Macerator with a second output slot that ramps up to 10× speed
- **Singularity Compressor** — a Compressor that ramps up to 10× speed
- **Centrifuge Extractor** — an Extractor with two extra output slots
- **Combined Recycler** — a Recycler with two extra output slots that keeps IC2's 1-in-8 scrap chance
- **Rotary Ore Washer** — an Ore Washing Plant with a fluid tank that ramps up to 10× speed
- Speed ramps up from the base IC2 rate to `maxMachineSpeedUpFactor` after `maxMachineSpeedUpTicks` of
  continuous work, and decays while idle
- Uses `machinePowerDrawFactor` times the base machine's EU/t while working, and declares the IC2
  voltage tier that draw belongs to (so the Rotary Ore Washer is MV/128 EU/t max at the default 3x,
  not a 48 EU/t machine advertising LV/32)
- Plays correctly with over-voltage rules (IC2Tweaks, IC2 1.12.2 packet model): a machine is never
  blown up by a supply of the class it actually draws from. Overclocking past the tier still needs
  transformer upgrades, exactly as in IC2
- Extra output slots are fully automation-friendly (bottom-side extraction, like IC2 outputs)
- A redstone signal idles a machine at zero cost
- Overclockers are capped at 6 per machine so a stacked upgrade slot can't hang the server
- IC2-style item tooltips (`Power X EU/t, Y EU/t max`), GUI chrome (compatible-upgrade info button
  and tooltip), wrench behaviour, and block drops. The `max` value is the voltage class derived from
  the advertised draw

## Requirements

| Dependency | Version | Notes |
| ---------- | ------- | ----- |
| Minecraft | 1.7.10 | — |
| Minecraft Forge | 10.13.4.1614 | [Download](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html) |
| [IndustrialCraft²](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) | 2.2.828-experimental | Required at runtime |

## Installation

1. Install **Minecraft Forge 10.13.4.1614** for Minecraft 1.7.10 (run the installer and select *Client*).
2. Download the latest **AdvancedMachines Legacy** jar from the [Releases](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/releases/latest) page.
3. Download [IndustrialCraft² 2.2.828-experimental](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) for 1.7.10.
4. Drop both jars into your `.minecraft/mods` folder.
5. Launch the game with the Forge profile.

## Downloads

- **[GitHub Releases](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/releases/latest)** — recommended; stable builds tagged from `main`.
- **[GitHub Actions](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/actions)** — bleeding-edge snapshot builds from every push to `main`.

## Configuration

`config/AdvancedMachines.cfg`:

| Option | Default | Description |
| ------ | ------- | ----------- |
| `maxMachineSpeedUpFactor` | `10` | Speed multiple reached at full ramp-up |
| `maxMachineSpeedUpTicks` | `10000` | Ticks of continuous operation to reach max speed |
| `machinePowerDrawFactor` | `3.0` | EU/t multiplier |
| `… Enabled` | `true` | Per-machine toggle; disabled machines have no recipe and are hidden from the creative tab |

Out-of-range values are repaired to the defaults when the config is loaded, so a bad edit cannot
divide-by-zero the speed logic or zero out the energy draw.

## Building from source

This project uses the [GTNewHorizons](https://github.com/GTNewHorizons) 1.7.10 Gradle toolchain. The correct JDK is provisioned automatically by Gradle — no manual Java setup required.

```bash
git clone https://github.com/MrFuzzihead/AdvancedMachines-Legacy.git
cd AdvancedMachines-Legacy
./gradlew build        # on Windows: gradlew.bat build
```

The built jars are placed in `build/libs/`. To run the unit tests or launch a development client:

```bash
./gradlew test         # on Windows: gradlew.bat test
./gradlew runClient    # on Windows: gradlew.bat runClient
```

## Contributing

Bug reports and pull requests are welcome! Please open issues on the [issue tracker](https://github.com/MrFuzzihead/AdvancedMachines-Legacy/issues).

- Branch naming follows `bugfix-<issue>-<short-name>` or `feature-<issue>-<short-name>`.
- CI runs the full build (including Checkstyle and Spotless checks) on every pull request, so please make sure `gradlew build` passes locally before opening a PR.
- Unit tests live in `src/test/java`; `./gradlew test` runs them.

## Changes in this fork

This fork continues [AtomicStryker's Advanced Machines](https://github.com/Jorch72/atomicstrykers-minecraft-mods) on the 1.7.10 IC2 branch. On top of that:

**Merged**

- Modernized build system for 2026 (GTNH convention plugin, Gradle wrapper, auto-provisioned JDK, GitHub Actions CI)
- Fixed the Rotary Macerator destroying outputs when a supplement was present, the output-slot stash check,
  the container slot-click item loss, the free-progress energy exploit, the dummy redstone output, and the
  recycler scrap chance
- Mining now drops the machine's contents (item/upgrade loss fixed via IC2's before-break stash approach)
- Wrench now rotates machines (front face removes) and no longer opens the GUI
- Washer speed sync, localised GUI labels, and a translated fluid tooltip
- Removed the deprecated reflection (progress field and sprite table) and the global identity recipes
- Overclockers are capped at insert time instead of exploding the machine
- IC2-parity pass: item tooltips, upgrade info button, `getGui()`/`getGuiContainer()`, inventory names
- 48 unit tests covering the speed/energy math, supplement/block/container logic and registration

## Credits

- **[AtomicStryker](https://github.com/Jorch72/atomicstrykers-minecraft-mods)** for the original Advanced Machines mod
- **[MrFuzzihead](https://github.com/MrFuzzihead)** and contributors for this 1.7.10 continuation
- **[GTNewHorizons](https://github.com/GTNewHorizons)** for the modern 1.7.10 build tooling
- The **IndustrialCraft²** team

## License

This project is licensed under the [MIT License](LICENSE). Advanced Machines is the work of its original authors; this fork only extends and maintains their work for 1.7.10.
