# IC2 AdvancedMachines (Legacy)

An [IndustrialCraft²](https://ic2.industrial-craft.net/) addon for **Minecraft 1.7.10** that adds five
upgraded versions of the standard IC2 processing machines.

| Machine | Base machine | Highlights |
|---|---|---|
| Rotary Macerator | Macerator | Extra output slot; ramps up to 10× speed |
| Singularity Compressor | Compressor | Ramps up to 10× speed |
| Centrifuge Extractor | Extractor | Two extra output slots |
| Combined Recycler | Recycler | Two extra output slots; keeps IC2's 1-in-8 scrap chance |
| Rotary Ore Washer | Ore Washing Plant | Fluid tank, ramps up to 10× speed |

All machines start at the base IC2 speed and accelerate while they keep working, up to
`maxMachineSpeedUpFactor` after `maxMachineSpeedUpTicks` ticks of continuous operation. They draw
`machinePowerDrawFactor` times the base machine's EU/t. A redstone signal idles a machine (no
progress, no energy use).

## Configuration

`config/AdvancedMachines.cfg`:

* `maxMachineSpeedUpFactor` — speed multiple reached at max ramp-up (default `10`)
* `maxMachineSpeedUpTicks` — ticks of continuous operation to reach max speed (default `10000`)
* `machinePowerDrawFactor` — EU/t multiplier (default `3.0`)

Out-of-range values are repaired to the defaults when the config is loaded, so a bad edit cannot
divide-by-zero the speed logic or zero out the energy draw.

## Building & testing

Requires a JDK and the Gradle wrapper:

```sh
./gradlew build          # compile + package
./gradlew test           # run the unit tests
./gradlew spotlessApply  # apply the project code style
```

The unit tests cover the speed/energy calculations, the recycler scrap chance, the drop/block
invariants and the tile entity output/consume behaviour without needing a running game.

## Credits

* AtomicStryker — original Advanced Machines mod
* MrFuzzihead and contributors — this 1.7.10 continuation
* The IC2 team
