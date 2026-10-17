# IC2 AdvancedMachines (Legacy) — Code Audit & IC2 Base-Class Comparison

**Target:** this repository (`atomicstryker.ic2.advancedmachines`, 1.7.10 Forge, branch `feature-2026Builds`)
**Compared against:** the actual IC2 `2.2.828-experimental` dev jar (the version pinned in `dependencies.gradle`), decompiled with CFR for this audit.
**Also compared against:** the upstream original source (`Jorch72/atomicstrykers-minecraft-mods`, `AdvancedMachinesAS`). A whitespace/format-insensitive diff shows **no logic differences**, so everything below is a real, long-standing issue in the mod — not an artifact of the recent decompile/reformat.

---

## 0. Executive summary

| # | Severity | Issue | Where |
|---|----------|-------|-------|
| 1 | **Critical** | Rotary Macerator supplement logic **deletes normal outputs** whenever anything sits in the supplement slot — **FIXED** (§1.1) | `TileEntityAdvancedMacerator.java:150-153` |
| 2 | **Critical** | `getOutput()` re-creates an output after the helper deliberately refused (output full / no water) → **input consumed, output voided** — **FIXED** (§1.2) | `TileEntityAdvancedMacerator.java:115-130` |
| 3 | **Critical** | Supplement slot click handler checks the **container slot index 8** (a player hotbar slot) instead of the supplement slot (index 44) → **deletes a player item** — **FIXED** (§1.3, override removed) | `ContainerAdvancedMacerator.java:22-35` |
| 4 | **High** | Speed hack **adds progress for free when the machine has no energy** → up to 10× speed while many progress ticks cost no EU — **FIXED** (§1.4) | `CommonLogicAdvancedMachines.java:129-152` |
| 5 | **High** | Progress is manipulated via **reflection**; failure silently disables speed and also disables the only guard on the dummy-redstone path — **FIXED** (§1.5) | `CommonLogicAdvancedMachines.java:29-56, 129-159` |
| 6 | **High** | Redstone "stall" injects a fake **dirt** `RecipeOutput`; can crash (`consume without a processing result`) or emit dirt in edge cases — **FIXED** (§1.6) | `CommonLogicAdvancedMachines.java:26, 75-115, 117-127` |
| 7 | **High** | Combined Recycler bypasses the base recycler's **1-in-8 scrap chance** → 8× scrap yield — **FIXED** (§1.7) | `TileEntityAdvancedRecycler.java:57-60` vs IC2 `TileEntityRecycler.operateOnce` |
| 8 | **High** | Mining drops the **IC2 basic machine block** (`blockMachine:0`), not the advanced machine (`:12`) the code intends — **FIXED** (§1.8) | `BlockAdvancedMachines.java:213-216` + missing `damageDropped` |
| 9 | **High** | Client icon table is the **IC2 1.6.4** table; reflection target no longer exists → wrong face textures for 12/36 (side, facing) pairs — **FIXED** (§1.9) | `client/AdvancedMachinesClient.java:21-33` |
| 10 | **Medium** | Macerator supplement state not persisted; unguarded stack math can NPE / go negative / leave 0-size phantom stacks — **FIXED** (§1.10) | `TileEntityAdvancedMacerator.java:201-217` |
| 11 | **Medium** | `isItemValidForSlot` / `canExtractItem` mix up legacy `oldStartIndex` with live inventory indexes → automation can pull upgrades out of the 3rd upgrade slot — **FIXED** (§1.11) | `TileEntityAdvancedMacerator.java:240-256` |
| 12 | **Medium** | Identity recipes are pushed into the **global** `Recipes.macerator`, affecting plain IC2 Macerators, and use an uncacheable stateful `IRecipeInput` hack — **FIXED** (§1.12) | `ModAdvancedMachines.java:125-131, 177-209` |
| 13 | **Medium** | Block/rendering robustness: no meta bounds check (AIOOBE), no TE null checks, `onBlockPlacedBy` runs on the client, dead `breakBlock` loop — **FIXED** (§1.13; GUI-close-on-break still open) | `BlockAdvancedMachines.java:127-140, 183-210, 228-252, 259-261` |
| 14 | **Low** | `machinePowerDrawFactor` applied after `super.setOverclockRates()`; no config validation (divide-by-zero kills speed) — **FIXED** (§1.14; validation + boosted `maxEnergy` sizing) | `CommonLogicAdvancedMachines.java:148-149, 161-177` |
| 15 | **Low** | Stale `@Mod(version/dependencies)`, hardcoded English GUI strings, no README, mcmod.info template leftovers — **MOSTLY FIXED** (§1.15/§4; GUI charge tooltips + creative filtering still open) | `ModAdvancedMachines.java:31-36`, GUIs, `README.md`, `mcmod.info` |

> **Reflection cleanup applied:** items **#5** and **#9** are resolved — both reflective accesses were removed without mixins. Notably, this was possible because neither access was actually blocked (`progress` is `protected`, and the 1.7.10 sprite mapping is exposed by IC2 as the public static `BlockBase.getTextureSubIndex(int, int)`). See the *Resolved* notes in §1.5 and §1.9. The speed formula's divide-by-zero crash path is guarded as part of the same change (§1.5 / §14).

---

## 1. Critical bugs

### 1.1 Macerator supplement logic destroys outputs

`TileEntityAdvancedMacerator.runSupplementLogic`:

```java
} else if (getSpecialResultFor(inputSlot.get(), result, supplement) != null) {  // line 150
    additions.addAll(getSpecialResultFor(inputSlot.get(), result, supplement)); // line 151
    supplementedItemsLeft = nextSupplementResultCount;
    iter.remove();                                                              // line 153 — kills real output
}
```

`getSpecialResultFor` (line 162) **never returns `null`** — it always returns an `ArrayList` (empty when nothing matched). So the condition on line 150 is **always true**. Consequences:

* With *any* item in the supplement slot, the first recipe result is removed and replaced by `additions`. If the supplement does not match the input (e.g. sand in the slot while macerating iron ore), `additions` is empty → **the normal maceration output is destroyed**.
* `nextSupplementResultCount` is only assigned inside a matching branch, so when nothing matches it keeps its previous (or initial `0`) value. `supplementedItemsLeft = 0` then the next operation destroys the output again — an endless, silent item sink until the supplement is removed.
* If `supplementedItemsLeft > 0` (line 147), the same removal happens with no "does it match?" check at all.
* Because `getSpecialResultFor` is invoked **twice** in the `else if` branch, recipe lookups and the `nextSupplementResultCount` / `nextSupplementResourceDrain` side effects run twice per item per tick. `getOutput()` itself runs at least once per tick and again in `operate()`'s loop.

**Fix:** have `getSpecialResultFor` return `null` (or an empty-optional) when nothing matched, and check `!additions.isEmpty()` before removing the original:

```java
List<ItemStack> special = getSpecialResultFor(inputSlot.get(), result, supplement);
if (!special.isEmpty()) {
    additions.addAll(special);
    supplementedItemsLeft = nextSupplementResultCount;
    iter.remove();
}
```

Also note the `items.isEmpty()` branch (lines 137-141) is **dead code**: it passes `result == null`, and `getSpecialResultFor` immediately short-circuits on `if (result != null && supplement != null)`, so additions can never be produced there.

### 1.2 `getOutput()` bypasses the "can the output be stored?" check

`CommonLogicAdvancedMachines.getOutput()` returns `null` not only when there is no recipe, but also when **no output slot can accept the result** (line 91-93) or when the washer's tank lacks fluid (95-101). `TileEntityAdvancedMacerator.getOutput()` treats `null` as "no base recipe" and re-runs the supplement logic anyway:

```java
RecipeOutput output = advLogic.getOutput(this);
if (!inputSlot.isEmpty()) {
    if (output == null) { output = new RecipeOutput(..., new ArrayList<>()); } // line 118-120
    ...
    runSupplementLogic(outItems);
    ...
    return output;   // may be a brand new output despite full slots
}
```

When the supplement matches, a fresh `RecipeOutput` is returned, `TileEntityStandardMachine.operate()` runs, `advLogic.operateOnce` consumes the input and then **cannot add the result to any slot** (line 120-125) → item destroyed. Reproduction: fill the Rotary Macerator's output slots, put quartz ore in the input and sand in the supplement slot.

**Fix:** separate "no recipe" from "not storable" (e.g. have the helper return a small result object/status), and never re-create an output when the stash check failed.

### 1.3 Supplement slot click uses the wrong index (player item loss)

`ContainerAdvancedMacerator`:

```java
addSlotToContainer(new Slot(tileEntity, TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX, 75, 17)); // TE index 8 — correct
...
public ItemStack slotClick(int slotIndex, ...) {
    if (slotIndex == TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX) {   // container index 8 — WRONG
        ItemStack slotItem = ((Slot) this.inventorySlots.get(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX)).getStack();
```

`slotClick`'s first parameter is the **container slot id**, not the tile-entity inventory index. Container layout for the Advanced Macerator:

| Container slots | Content |
|---|---|
| 0–35 | player inventory (36 slots, `ContainerFullInv.addPlayerInventorySlots`) |
| 36 | discharge slot |
| 37 | input |
| 38 | base output |
| 39–42 | 4 upgrade slots |
| 43 | extra output (`outputextra1`) |
| 44 | **supplement slot** |

So the check fires when the player clicks **container slot 8 = the 9th hotbar slot** while `supplementedItemsLeft != 0`, and decrements that player stack (`slotItem.stackSize--`, line 30) — losing a player item and resetting the supplement counter. Clicking the *real* supplement slot (44) never runs this code.

**Fix:** compare the underlying slot, e.g.

```java
Slot slot = this.inventorySlots.get(slotIndex);
if (slot != null && slot.inventory == advMacerator
        && slot.getSlotIndex() == TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX) { ... }
```

Better still: drop this client/server `slotClick` hack entirely — `TileEntityAdvancedMacerator.operateOnce()` already clears `supplementedItemsLeft` when the supplement slot becomes empty (lines 201-203).

### 1.4 Free progress when the machine cannot pay for it (energy exploit)

`CommonLogicAdvancedMachines.updateEntity()`:

```java
if (te.getActive()) speed++; else speed--;
if (runningEmpty) {
    progressField.set(te, (short) 0);
} else {
    short extraprogress = (short) Math.round(speed / (mod.maxMachineSpeedUpTicks / (mod.maxMachineSpeedUpFactor - 1)));
    progressField.set(te, (short) (progressField.getShort(te) + extraprogress));
}
```

The base `TileEntityStandardMachine.updateEntityServer()` only consumes EU and advances `progress` inside the branch `output != null && energy >= energyConsume`. When a machine has a valid output but **less energy than the (already 3×-multiplied) consumption**, the base does nothing — but `runningEmpty` is `false`, so the mod still injects up to `maxMachineSpeedUpFactor - 1` extra progress ticks **for free, every tick**. Measured effect at defaults (`factor = 10`, `ticks = 10000`, `draw = 3`): a starved machine can complete operations at ~10× speed while many of its progress ticks cost no EU at all — in a deliberately starved setup the EU per operation can fall to the order of `1/30` of the base. It also banks progress while idle (the extra ticks are added even when the machine is inactive).

**Fix options:** only add progress on ticks where the base *actually* advanced (e.g. track `progress` before/after `super.updateEntityServer()` and gate on that), or better, implement speed as an `operationLength` reduction (see §6) so IC2's own accounting handles it.

### 1.5 Reflection on `progress` is unnecessary and its failure mode is bad

```java
progressField = TileEntityStandardMachine.class.getDeclaredField("progress");
progressField.setAccessible(true);
```

* In IC2 2.2.828 the field is `protected short progress` — **directly accessible from subclasses**. Every `TileEntityAdvanced*` is a subclass and could expose it. The reflection is pure legacy debt.
* It runs `getShort`/`set` reflectively **for every advanced machine, every tick** (allocating boxes, doing access checks) — a real cost with dozens/hundreds of machines.
* If the lookup ever fails (mapping changes, obfuscated IC2, another fork), the catch prints a stack trace and `failed = true`: **all speed functionality silently disappears** — and, worse, `advLogic.updateEntity` no longer resets `progress` while `runningEmpty`, so the dummy-redstone path (§1.6) becomes reachable for every machine.
* The arithmetic can throw (`maxMachineSpeedUpFactor <= 1` → integer division by zero; `== 1` → divide by zero). On any exception `progressField` is set to `null` and never re-acquired for existing TEs.

**Fix:** add a `protected void setRawProgress(short)` / `getRawProgress()` accessor to an abstract advanced-machine base class (which *is* a subclass of `TileEntityStandardMachine`), and have the helper take that interface/lambda. Validate config values at load time.

> **Resolved (reflection removed, no mixins).** Implemented as a small `IProgressAccess` interface (`getRawProgress()` / `setRawProgress(short)`) which all five `TileEntityAdvanced*` classes implement by reading/writing the inherited `protected short progress` directly. `CommonLogicAdvancedMachines.updateEntity(TileEntityStandardMachine)` now casts the machine to `IProgressAccess` and calls the accessors; the `Field`, `failed`, and `setAccessible(true)` machinery are gone. This also removes the per-tick reflective boxing/access-check cost and the "lookup failed → speed silently disabled" failure mode. As a bonus the `maxMachineSpeedUpFactor <= 1` / `maxMachineSpeedUpTicks` divide-by-zero is guarded, and the original integer-division speed math is preserved. The deeper behavioural problems (§1.4 free progress, §1.6 dummy dirt output) are *not* addressed by this cleanup.

### 1.6 The dummy dirt output (redstone behavior is inverted and unsafe)

`CommonLogicAdvancedMachines` line 26:

```java
private final RecipeOutput dummyOut = new RecipeOutput(new NBTTagCompound(), new ItemStack(Blocks.dirt));
```

`getOutput()` returns it whenever the machine is redstone-powered and can't produce/stash a real result (lines 108-112), and `runningEmpty` is used to force `progress = 0`. Problems:

* **The semantics are inverted vs. a redstone upgrade.** If the input *is* processable and the output has room, `runningEmpty = false` and the machine **keeps running while powered**. It only stalls on invalid inputs or a full output. Also, when it stalls it still consumes EU (the base sees a non-null output and charges `energyConsume`) and still ramps `speed` (the base sets `active = true`). IC2's `Redstone` component / inverter upgrade simply makes the machine idle at zero cost.
* If a machine ever reaches `progress >= operationLength` while `getOutput()` returns `dummyOut`, `advLogic.operateOnce` runs `te.inputSlot.consume()` and then adds `processResult` (= **dirt**) to the first accepting slot. `InvSlotProcessableGeneric.consume()` throws `IllegalStateException("consume without a processing result")` when the input has no recipe; for a processable input it dequeues the input and then tries to insert dirt (item loss if dirt does not fit, an actual dirt output if it does). This is normally prevented by the `runningEmpty → progress = 0` reset, but the reset runs *after* the base tick and the mod's own extra progress can leave `progress == operationLength` at a tick boundary; it also disappears completely if the reflection fails (§1.5).
* The dirt stack is `new ItemStack(Blocks.dirt)` — a real item that can be merged into output slots.

**Fix:** never return a fake output. Return `null` when powered (the machine idles; then make sure the extra-progress injection is also gated on redstone), or implement IC2's redstone-modifier idiom. If the "stall" is desired, use a boolean getter instead of a recipe.

### 1.7 Combined Recycler ignores the base recycler's scrap chance

IC2 `TileEntityRecycler.operateOnce`:

```java
public void operateOnce(RecipeOutput output, List<ItemStack> processResult) {
    this.inputSlot.consume();
    if (IC2.random.nextInt(recycleChance()) == 0) {   // recycleChance() == 8
        this.outputSlot.add(processResult);
    }
}
```

`TileEntityAdvancedRecycler.operateOnce` (lines 57-60) delegates straight to `advLogic.operateOnce`, which always `slot.add(processResult)`. Result: **1 scrap per input instead of 1 per 8 inputs**, before the speed multiplier (which stacks multiplicatively, so up to ~80× base throughput). If this is intended as an upgrade, it should be a documented config option; otherwise it should reproduce the base probability:

```java
@Override
public void operateOnce(RecipeOutput output, List<ItemStack> processResult) {
    if (!inputSlot.isEmpty()) inputSlot.consume();
    if (IC2.random.nextInt(TileEntityRecycler.recycleChance()) == 0) {
        // add to the first accepting output slot
    }
}
```

### 1.8 Mining drops the wrong item (basic machine block, not advanced machine)

`BlockAdvancedMachines`:

```java
@Override
public Item getItemDropped(int meta, Random random, int fortune) {
    return IC2Items.getItem("advancedMachine").getItem();   // ItemBlock of IC2 blockMachine
}
// getDamageValue(...) is overridden, but damageDropped(int) is NOT
```

Vanilla `Block.getDrops` creates `new ItemStack(getItemDropped(...), 1, damageDropped(meta))`, and `damageDropped` defaults to `0`. `IC2Items.getItem("advancedMachine")` is `blockMachine:12`, so the drop becomes **`blockMachine:0` — the basic IC2 machine casing**. The author almost certainly intended `blockMachine:12` (the advanced machine component), or the mod's own machine item at `damage = meta`.

Also relevant: IC2's own `BlockMachine.damageDropped` respects `balance/ignoreWrenchRequirement` (returns the real meta when that config is on); the mod hard-codes the wrench-required behavior.

**Fix (pick one):**

```java
@Override public int damageDropped(int meta) { return 12; } // IC2 advanced machine component
```
or return the mod's own block item (`Item.getItemFromBlock(this)`) with `meta` and let the wrench drop logic be identical to mining.

### 1.9 Client sprite mapping is the IC2 1.6.4 table and its reflection target is gone

```java
sideAndFacingToSpriteOffset = (int[][]) Class.forName("ic2.core.block.BlockMultiID")
        .getField("sideAndFacingToSpriteOffset").get(null);
} catch (Exception e) {
    sideAndFacingToSpriteOffset = new int[][] { { 3, 2, 0, 0, 0, 0 }, ... };
```

In IC2 2.2.828:

* `BlockMultiID` has **no such field**; the table is `private static final int[][] facingAndSideToSpriteOffset` in `ic2.core.block.BlockBase`, and it is indexed `[facing][side]`, not `[side][facing]`.
* The hardcoded fallback is the old **IC2 1.6.4** `sideAndFacingToSpriteOffset` table.

Comparing the mod's fallback (transposed) with 2.2.828's `BlockBase.facingAndSideToSpriteOffset` programmatically shows **12 of 36 (side, facing) combinations map to a different sprite index**. Most mod machines happen to use the same texture for indices 2/4/5 (`sideplate`), so the visible damage is limited to the one machine with a distinct back texture — the **Rotary Ore Washer** (`back32` vs `sideOreWasherOFF`) — but the mapping is objectively wrong for all of them and will produce wrong faces whenever textures diverge.

**Fix:** delete the reflection and copy the 2.2.828 table with correct orientation, or (internally) call `ic2.core.block.BlockBase.getTextureSubIndex(facing, side)` — being aware that it is IC2-internal, not API. Note the field is also `private`, so `getField` could never find it anyway.

> **Resolved (reflection removed, no mixins).** The dead `Class.forName("ic2.core.block.BlockMultiID").getField("sideAndFacingToSpriteOffset")` lookup and the wrong 1.6.4 fallback table were deleted from `AdvancedMachinesClient` (its `load()` is now a NOOP). `BlockAdvancedMachines.getIcon(IBlockAccess, …)` now uses IC2's own `BlockBase.getTextureSubIndex(facing, blockSide)` and adds `+6` for the active texture set, exactly as `BlockMultiID.getIcon` does; the inventory `getIcon(int, int)` uses `getTextureSubIndex(3, blockSide)` (IC2's default facing). This fixes the 12/36 wrong face mappings, including the Rotary Ore Washer's `back32` back face, and removes the last client-class reference from the common block class.

---

## 2. Machine-by-machine comparison against the IC2 base machines

Base parameters in IC2 **2.2.828-experimental** (decompiled from the exact jar pinned in `dependencies.gradle`):

| Base machine | EU/t | Operation length | Slots | Wrench drop |
|---|---|---|---|---|
| `TileEntityMacerator` | 2 | 300 ticks | 1 in / 1 out / 4 upgrades | 0.85 |
| `TileEntityCompressor` | 2 | 300 ticks | 1 in / 1 out / 4 upgrades | 0.85 |
| `TileEntityExtractor` | 2 | 300 ticks | 1 in / 1 out / 4 upgrades | 0.85 |
| `TileEntityRecycler` | 1 | 45 ticks | 1 in / 1 out / 4 upgrades | 0.85 |
| `TileEntityOreWashing` | 16 | 500 ticks | 1 in / **3 out** / fluid slot / cell slot / 8-bucket tank | 0.85 |

All five advanced machines apply the same three global changes:

1. `setOverclockRates()` multiplies `energyConsume` by `machinePowerDrawFactor` (default **3×**).
2. `advLogic.updateEntity()` injects up to `maxMachineSpeedUpFactor - 1` extra progress ticks per tick (ramping to **10×** after `maxMachineSpeedUpTicks` = 10000 ticks).
3. Output is routed to the first accepting slot among an extended list (`advLogic.getOutputSlots()`), instead of only the base output slot.

### 2.1 Rotary Macerator vs Macerator

| Aspect | Base | Advanced | Assessment |
|---|---|---|---|
| Recipes | `Recipes.macerator` | same | ✅ |
| Slot layout (inventory indexes) | discharge 0, output 1, upgrades 2-5, input 6 | + extra output 7, + supplement 8 | ⚠️ index confusion, see §1.3/§4 |
| Wrapper / sounds / GUI container | IC2 | inherits; custom GUI | ✅ |
| Speed | base | progress injection | ⚠️ energy exploit §1.4, reflection §1.5 |
| Power | 2 EU/t | 6 EU/t | ⚠️ `maxEnergy` still sized for 2 EU/t × 300 (600 EU) |
| Supplements | n/a | bronze dust, 2 quartz, glowstone, hydrated coal | ❌ **destroys outputs** §1.1; unpersisted state §1.10; NPE risks §1.10 |
| Redstone | ignore unless upgraded | fake dirt output / inverted | ❌ §1.6 |
| Extra outputs | 1 slot | 2 slots (primary + extra), auto-swapped each tick | ⚠️ the "swap" is redundant and bypasses `InvSlotOutput` semantics |
| Comparator | `getProgress()*15` | same (inherited) | ✅ but `getProgress()` is one tick stale after the injected extra progress |

Notes:

* `updateEntityServer()` performs a per-tick `put()` swap moving the extra output into the primary output (lines 86-105). This is unnecessary — `advLogic.operateOnce` already routes into the first accepting slot — and it defeats the purpose of a second output slot (it can never hold a second stack for more than a tick if the primary is empty).
* The identity recipes (`netherrack → netherrack`, `quartz_ore → quartz_ore`) are registered **globally**; they make *plain* IC2 Macerators "process" those items (wasting power, outputting the input) and exist only to satisfy the supplement logic's "must have a base recipe" assumption. See §1.12.

### 2.2 Singularity Compressor vs Compressor

| Aspect | Base | Advanced | Assessment |
|---|---|---|---|
| Recipes | `Recipes.compressor` | same | ✅ |
| Slots | 1 in / 1 out / 4 upgrades | same (no extra output) | ✅ |
| Speed/power | — | 10× ramp / 3× EU | ⚠️ §1.4, §1.5 |
| Redstone | ignored | fake dirt output, inverted | ❌ §1.6 |
| Output routing | `outputSlot.canAdd` | `advLogic` (same slot) | ✅ |
| `getOutput()` | base checks `outputSlot.canAdd` | helper checks all output slots | ✅ (only one output slot exists) |
| GUI | `GuiCompressor` | `GuiSingularityCompressor` | ✅ but hardcoded English strings |

This is the cleanest of the five; only the shared speed/redstone issues apply.

### 2.3 Centrifuge Extractor vs Extractor

| Aspect | Base | Advanced | Assessment |
|---|---|---|---|
| Recipes | `Recipes.extractor` | same | ✅ |
| Slots | 1 in / 1 out / 4 upgrades | +2 extra outputs (inv indexes 7, 8) | ✅ (container adds them correctly) |
| Speed/power/redstone | — | same shared issues | ⚠️/❌ §1.4-§1.6 |
| `getOutput()` fallback | base checks `outputSlot.canAdd` | `advLogic` checks all 3 slots | ✅ |

Nothing extractor-specific is broken; it benefits from the extra output slots for recipes with multiple outputs.

### 2.4 Combined Recycler vs Recycler

| Aspect | Base | Advanced | Assessment |
|---|---|---|---|
| Recipes / blacklist | `Recipes.recycler`, `recyclerBlacklist/Whitelist` | same manager | ✅ |
| Slots | 1 in / 1 out / 4 upgrades | +2 extra outputs | ✅ |
| **Scrap chance** | **1/8 per operation** | **1/1** | ❌ §1.7 |
| Speed/power/redstone | — | same shared issues | ⚠️/❌ |
| Speed | 1 EU/t × 45 ticks | 3 EU/t, up to 10× rate | the 1/8 loss compounds badly (§1.7) |

### 2.5 Rotary Ore Washer vs Ore Washing Plant

| Aspect | Base | Advanced | Assessment |
|---|---|---|---|
| Recipes | `Recipes.oreWashing` | same | ✅ |
| Tank / fluid intake | 8000 mB, `gainFluid()` | inherits | ✅ |
| Slots | 1 in / **3 out** / fluid / cell / upgrades | same (adds the whole base `outputSlot` to the helper list) | ✅ |
| Container | `ContainerOreWashing` | same base container | ✅ (avoids the macerator's index bugs) |
| Speed | — | `updateEntityServer` **does** call `advLogic.updateEntity` (verified) | ⚠️ shared energy exploit |
| `operateOnce` | `super.operateOnce(...)` then `fluidTank.drain(metadata.amount)` | `advLogic.operateOnce(...)` then `fluidTank.drain(output.metadata.getInteger("amount"))` | ⚠️ see below |
| Redstone | ignored | fake dirt output | ❌ §1.6 — and here `drain(0)` is a no-op rather than a guard |

Washer-specific notes:

* The base `TileEntityOreWashing.getOutput()` returns `null` if `metadata == null` and if `metadata.amount > tank amount`. The mod's helper only does the tank check and **not** the `metadata == null` check; if a recipe ever returns null metadata, `operateOnce` will NPE on `output.metadata.getInteger(...)` (line 56). No vanilla IC2 recipe does this, so it is a latent robustness bug.
* The base drains the tank inside its own `operateOnce`; the mod reimplements it. If `advLogic.operateOnce` refuses to add output (shouldn't happen after §1.2's fix, but currently possible), the tank is still drained.

### 2.6 Cross-machine deviations that should be aligned with the base

* **Speed is implemented as raw `progress` injection, not as IC2's `operationLength`/`operationsPerTick` model.** This bypasses `getProgress()`/`guiProgress` (GUI lags), the comparator, and IC2's own overclocker math, and creates the free-energy hole (§1.4). Doing `operationLength = round(operationLength / speedFactor)` inside `setOverclockRates()` after `super` (and recomputing `progress`) would use IC2's own accounting and delete the need for reflection, `dummyOut`, and most of the shared logic.
* **`energyConsume` is multiplied after `super.setOverclockRates()`**, so `maxEnergy = defaultEnergyStorage + operationLength * energyConsume` is computed with the *old* value. The internal buffer is 3× too small relative to the draw; a powered machine with a full buffer can only sustain ~1/3 as many ticks. Consider `te.maxEnergy = Math.max(te.maxEnergy, te.operationLength * te.energyConsume)` after the multiplication.
* **Extra output slots are good** (`InvSlotOutput`, bottom-side extraction, correct container slots) and match IC2 conventions.
* **Overclocker cap:** the mod creates an explosion when > 6 overclockers are installed. IC2 has no such concept; the explosion is unreliable (strength 3 may not remove the block), fires repeatedly on every `setOverclockRates()` (i.e. every inventory change) and does not prevent the real hazard: IC2's `InvSlotUpgrade` allows **stacked** overclockers, and with enough of them `operationsPerTick` explodes into the millions, hanging the server inside `operate()`. A hard cap enforced at insert time (rejecting the item / limiting slot stack size) would be safer than an explosion.
* **`getInventoryName()`** is inherited from the base machines ("Macerator", "Compressor", …), so WAILA/NEI/other integrations label the advanced machines with the base names. Overriding it (or the tile registration names) is a small consistency win.
* **`getGui()`** is also inherited and returns the *base* GUI; harmless today because the mod's own `IGuiHandler` is used, but any mod that opens an IC2 machine GUI directly (`IHasGui.getGui`) would get the wrong screen.

---

## 3. Block, rendering and drop issues

`BlockAdvancedMachines` extends `BlockContainer` instead of IC2's `BlockMachine`/`BlockMultiID`, so it manually re-implements block behavior and misses IC2's hooks. Issues:

1. **No meta bounds checking** (`iconBuffer` is `[5][12]`):
   * `getIcon(int, int)` line 139 and `getIcon(IBlockAccess, …)` line 133-134 index `iconBuffer[blockMeta]` directly → `ArrayIndexOutOfBoundsException` for meta > 4 (corrupt metadata, other mods, `/setblock`). IC2's version checks `index >= textures.length` and returns `null`.
2. **No TE null checks:** `isActive()` (line 259-261) casts `iba.getTileEntity(...)` unconditionally; `getIcon(IBlockAccess,…)`/`randomDisplayTick` call it. A missing TE (chunk edge, fake `IBlockAccess`, block-entity moved) is an NPE. IC2 uses `getOwnTe()` with logging/fallback.
3. **`onBlockPlacedBy` runs on the client:** IC2's `BlockMultiID.onBlockPlacedBy` returns early when `!IC2.platform.isSimulating()`; the mod sets facing on both sides, triggering a client-side `updateTileEntityField` network send. It also casts the TE without a null check.
4. **`breakBlock` is dead code with a duplication risk:** `super.breakBlock` (`BlockContainer`) removes the TE first, so the subsequent `getDrops(...)` sees no inventory, the loop skips element 0 and then returns (lines 183-210). Nothing is ever spawned by it; the real drops come from vanilla `dropBlockAsItemWithChance → getDrops`. Should the removal order ever change (e.g. moving to IC2's block base), this would spawn **one duplicate inventory stack per break**. Delete it, or port IC2's `onBlockPreDestroy` (which also closes open GUIs and calls the TE break hook).
5. **`getDrops` does not clear slots** and is called from more than one path (vanilla harvest and IC2's wrench). It relies on the TE being removed afterwards — the same design as IC2 — but combined with #4 it is fragile. It also exposes the *supplement* slot and *upgrade* slots as drops (fine).
6. **Missing IC2-equivalent behaviors:** `canCreatureSpawn` (mobs can spawn on these machines, IC2 machines explicitly return `false`), `onNeighborBlockChange → TileEntityBlock.onNeighborUpdate` (components won't see neighbor changes), IC2 `renderMask`/`shouldSideBeRendered`/`onRender` (only relevant for non-opaque or TESR blocks). `hasComparatorInputOverride` and `getComparatorInputOverride` are present and match IC2 (`floor(getProgress() * 15)`).
7. **`rotateBlock` is effectively a no-op:** IC2's `TileEntityBlock.wrenchCanSetFacing` returns `false` (verified in the 2.2.828 bytecode) and `TileEntityStandardMachine` does not override it, so `te.wrenchCanSetFacing(null, newFacing)` is always false. This matches IC2's own `BlockMultiID.rotateBlock` behavior (also returns false), so it is consistent — but the method can be removed or made to actually rotate the machine by overriding `wrenchCanSetFacing` in the advanced TEs.
8. **`getDrops`/`getItemDropped`/`damageDropped` mismatch** — see §1.8.
9. **No `setBlockName`** in the constructor; `GameRegistry.registerBlock(block, ItemAdvancedMachine.class, "blockAdvMachine")` sets it. Fine for the item (which overrides its name), but the block's unlocalized name is only what FML assigns.

---

## 4. Container / GUI / networking

1. **`ContainerAdvancedMachine`** is used for the Macerator, Compressor, Extractor and Recycler and maps `advM.getOutputSlots().size()` (1/2/3) to the extra container slots. That logic is correct for the current slot lists.
2. **`ContainerAdvancedMacerator.slotClick` index bug** — §1.3.
3. **Speed sync** uses vanilla progress bars with index `3` (`sendProgressBarUpdate` / `updateProgressBar`). This works with 2.2.828 (`ContainerBase` uses IC2's networked-field mechanism for `guiProgress`, `guiChargeLevel`, `tier`, `fluidTank`, not progress bars), but it is version-fragile and only updates while a container is open. IC2's `getNetworkedFields()`/`onNetworkUpdate` idiom would be more idiomatic (expose the speed on the TE and add it to `getNetworkedFields`). Also `speedNetworked` is only set in `detectAndSendChanges` (server side) — fine.
4. **Hardcoded English strings** in all five GUIs: `"Inventory"`, `"Speed:"`, `"Pressure:"`, `"Mass rate:"`. The mod ships `en_US`, `de_DE`, `ru_RU` lang files that are never used for these. Use `StatCollector.translateToLocal("container.inventory")` and add keys.
5. **The washer GUI has no speed readout** (`GuiRotaryOreWasher` draws only the title and the fluid tooltip), inconsistent with the other four.
6. **`GuiRotaryOreWasher` fluid tooltip** uses `fluidstack.getFluid().getName()` (internal name, untranslated) and dereferences `getFluid().getFluid().getIcon()`; if the `Fluid` object is missing (unregistered/removed fluid) these NPE. IC2's own GUI guards and uses `"ic2." + fluidName` for translation.
7. **`ModAdvancedMachines.getServerGuiElement`** casts `world.getTileEntity(...)` to `TileEntityStandardMachine` without an `instanceof` check — a stale GUI packet after the block was replaced, or another mod's block, throws `ClassCastException`. Cheap to harden.
8. **GUIs extend `GuiContainer` directly**, so they lose IC2's `GuiMachine` niceties (charge/tier tooltips, side info, `drawTooltip` helpers for upgrades). The custom textures make this acceptable, but the charge tooltip in particular is a usability regression.
9. **Extra output slot coordinates** are (115, 17)/(115, 53) while the base machine's output is at (116, 35) — 1 px offset that only matters for the mod's own textures (they are drawn for it). Cosmetic.

---

## 5. Optimizations

| Area | Current | Suggestion |
|---|---|---|
| Progress access | reflection `getShort`/`set` per machine per tick | `protected` accessor on a shared abstract TE base; call it directly |
| Recipe lookup for supplements | `Recipes.macerator.getOutputFor(supplement, false)` + `supplement.copy()` called 2-6× per tick per machine (`getSpecialResultFor` invoked twice per item, `getOutput` invoked at least twice per tick) | Cache by `(supplement item, input item)` / null-check-first; call `getSpecialResultFor` once and reuse the result; skip entirely when the supplement slot is empty |
| Allocations | `advLogic.getOutput()` + `runSupplementLogic` + `new RecipeOutput` + `new ArrayList` per tick per machine; `dummyOut` per instance | Reuse buffers, early-out when the input hasn't changed; make per-machine constant `ItemStack`s static/final (macerator allocates ~10 constant stacks per TE instance, lines 57-70) |
| Identity recipes | two stateful, **uncacheable** `IRecipeInput`s in IC2's `BasicMachineRecipeManager` → every failed macerator lookup linearly scans `uncacheableRecipes` and calls `matches()` | Use normal `RecipeInputItemStack` with `metadata.setBoolean("ignoreSameInputOutput", true)` (2.2.828 supports it), or (better) handle the supplement inputs entirely inside the advanced TE and don't pollute the global recipe manager |
| `speed` ramp | field updated every tick per machine | negligible, but could be ticked less often for idle machines |
| Block icon lookup | `getIcon` re-fetches the TE twice (`isActive` + facing) | Fetch once and pass it around |
| `getSpecialResultFor` | performs a full recipe-manager scan to answer "is the supplement tin ore?" | Compare with `Ic2Items.crushedTinOre` / `OreDictionary` directly instead of macerating the supplement |
| `TileEntityAdvancedMacerator` fields | per-instance `ItemStack` constants | make them `private static` (after IC2 item registration) or store item references |

---

## 6. Recommended modernization (how to extend the IC2 machines properly)

1. **Introduce one abstract base class**, e.g. `abstract class TileEntityAdvancedMachine extends TileEntityStandardMachine implements IAdvancedMachine`, holding:
   * the `CommonLogicAdvancedMachines` helper (replace `implements IAdvancedMachine` on the helper — it is a helper, not a machine),
   * `updateEntityServer`, `setOverclockRates`, `getOutput`, `operateOnce`, `printFormattedData`, `getSpeed`, `setClientSpeed`, `getOutputSlots`,
   * typed accessors for the raw progress (`this.progress` is `protected` — direct access works).
   The five machine classes then shrink to constructors + machine-specific extras. This removes ~40 duplicated lines each and makes the shared behavior fixable in one place.
2. **Implement speed through IC2's own knobs.** In `setOverclockRates()` (which the base already calls on load/changes), after `super.setOverclockRates()`, scale `operationLength` down and keep `progress` proportional. Benefits: no reflection, no `dummyOut`, no free-progress exploit, GUI/comparator/IC2-overclocker math all stay consistent, and the energy accounting is IC2's (`maxEnergy` grows with `operationLength * energyConsume`).
3. **Redstone:** either leave redstone to IC2 upgrades, or return `null` from `getOutput()` while powered (machine idles, no EU, no progress). Delete the dirt output.
4. **Recipe/supplement handling:** duplicate the macerator recipe *inside the advanced machine* when the supplement slot is occupied (build the supplemental output list after `inputSlot.process()` succeeds), return `null` on non-match, and never register identity recipes globally. Persist `supplementedItemsLeft`/queued drain state to NBT; make the drain handling null/negative-safe.
5. **Block:** extend `ic2.core.block.machine.BlockMachine` (or copy its missing hooks) to inherit IC2 registration, texture scheme, wrench/drop behavior, `onBlockPreDestroy` GUI closing and creature-spawn blocking; alternatively port:
   * bounds-checked `getIcon`,
   * `onBlockPlacedBy` simulation-side guard,
   * `damageDropped` (12 / meta / config-aware),
   * delete the dead `breakBlock` loop.
6. **Client:** replace the stale sprite table; keep client-only code out of the common block class (the server proxy's `load()` is a no-op, so `AdvancedMachinesClient.sideAndFacingToSpriteOffset` is `null` on a dedicated server — `getIcon` would NPE if it were ever called there).
7. **Config:** validate `maxMachineSpeedUpFactor >= 2`, `maxMachineSpeedUpTicks > 0`, `machinePowerDrawFactor > 0`; only register the identity/supplement recipes when the machine is enabled; optionally hide disabled machines from creative (`ItemAdvancedMachine.getSubItems` currently always adds all 5).
8. **Metadata/build:** use `Tags.VERSION` in `@Mod(version=…)` (currently hardcoded `"1.1.6"` while the build produces `61.0.3`), update `dependencies` to `IC2@2.2.828`, fix the `UniMixins` entry in `mcmod.info` (mixins are disabled), and write the empty `README.md`.

---

## 7. Smaller issues / nitpicks

* `TileEntityAdvancedMacerator.MAIN_IN_SLOT_INDEX` (line 21) is unused.
* `ItemAdvancedMachine.getUnlocalizedName` returns `null` for damage > 4 → `Item.getItemStackDisplayName` produces `"null.name"` or an NPE depending on the caller; clamp to a fallback.
* `ItemAdvancedMachine` sets its creative tab to `tabBlock` while the block sets `tabRedstone` — the item wins; pick one.
* `ItemAdvancedMachine.getSubItems` ignores the config-enabled flags.
* `CommonLogicAdvancedMachines.readFromNBT` swallows all exceptions with an empty `catch`.
* `CommonLogicAdvancedMachines` captures `ModAdvancedMachines.instance` at TE-construction time; if a TE were ever constructed before `@Instance` injection it would NPE. Prefer reading it lazily.
* The macerator's `twoQuartz` / `hydratedCoalDust` are shared `ItemStack` instances handed to `RecipeOutput`; copy them before handing them out to be safe against mutation by upgrade/liquid code.
* `GuiCentrifugeExtractor`, `GuiCombinedRecycler`, `GuiSingularityCompressor`, `GuiRotaryOreWasher` take an unused `te` parameter.
* `ModAdvancedMachines` reads config keys in `postInit` and saves there; a crash before post-init loses the config (minor).
* `Random` spam in `randomDisplayTick` duplicates IC2's `BlockMachine` code (fine), but `isActive` lacks the null check (see §3).
* `BlockAdvancedMachines` uses `soundTypeAnvil` while IC2 machines use `soundTypeMetal` — audible inconsistency, not a bug.

---

## 8. Things that are (surprisingly) fine — avoid "fixing" these

* The supplement slot **is** at TE inventory index 8, so `new Slot(tileEntity, 8, …)` and `isItemValidForSlot(8, …)` target the right inventory entries. The bug is only the *container* index in `slotClick`.
* All `IC2Items.getItem(...)` names used (`crushedCopperOre`, `crushedTinOre`, `coalDust`, `waterCell`, `bronzeDust`, `hydratedCoalDust`, `advancedMachine`, all recipe ingredients) exist in 2.2.828.
* Wrench drops, sounds, upgrade handling, energy handling and `getUpgradableProperties` are inherited correctly from the base machines (0.85 wrench-drop rate included).
* The mod's own `IRecipeInput.matches` hack *does* get past IC2's input==output check, and its one-time `false` is mostly harmless — but it is unnecessary in 2.2.828 (see §5).
* `getDrops` + vanilla `dropBlockAsItemWithChance` do drop the machine's inventory correctly (the custom `breakBlock` is dead code, not a dupe, in the current call order).
* `operationsPerTick` stays 1 for all normal (non-overclocked) configurations, so the base `operate()` loop does not multiply the mod's supplement side effects per call.

---

## 9. Suggested fix order

1. §1.1 supplement output destruction — one-line logic fix, stops silent item loss.
2. §1.3 container slot index — stops player item loss.
3. §1.2 stash-check bypass — stops item loss when outputs are full.
4. §1.4 energy exploit (gate extra progress on a base-progress change).
5. §1.6 dummy dirt output / redstone semantics.
6. §1.7 recycler chance, §1.8 drop damage.
7. §1.5 reflection removal (do together with §6.2 operationLength-based speed).
8. §1.9 icon table, §3 robustness (bounds/null checks, delete dead `breakBlock`), §4 localization.
9. §1.12 recipe-manager pollution, §1.10 persistence/state hardening, §1.11 automation index mix-up.
10. Modernization refactor (§6) and config/build cleanup.

---

## Appendix A — IC2 2.2.828 reference values used in this audit

* `TileEntityStandardMachine(energyPerTick, length, outputSlots, defaultTier=1)`; fields: `protected short progress`, `public int energyConsume`, `public int operationLength`, `public int operationsPerTick`, `public InvSlotProcessable inputSlot`, `public final InvSlotOutput outputSlot`, `public final InvSlotUpgrade upgradeSlot`; `getProgress()` returns `guiProgress`, updated at the end of `updateEntityServer()`.
* Energy is deducted and `progress` advanced only inside `if (output != null && energy >= energyConsume)`.
* `InvSlotProcessableGeneric.consume()` throws `IllegalStateException` if the recipe manager returns `null` ("consume without a processing result") and clears the slot when `stackSize <= 0`.
* `InvSlotOutput.canAdd(...)` is all-or-nothing (returns true only if the whole list fits) and `add(...)` copies stacks.
* `TileEntityInventory.isItemValidForSlot(int index, …)` / `canExtractItem(int index, …)` take the **inventory index** (sum of `InvSlot.size()` in registration order), while `InvSlot.oldStartIndex` is only used for legacy NBT migration.
* `TileEntityBlock.wrenchCanSetFacing` returns `false`; `wrenchCanRemove` returns `true`; `TileEntityMacerator/Compressor/Extractor/Recycler/OreWashing` define `getWrenchDropRate() == 0.85f`.
* `BlockBase.facingAndSideToSpriteOffset[facing][side]`:
  `{3,5,1,0,4,2}, {5,3,1,0,2,4}, {0,1,3,5,4,2}, {0,1,5,3,2,4}, {0,1,2,4,3,5}, {0,1,4,2,5,3}`.
* `TileEntityRecycler.operateOnce` outputs only when `IC2.random.nextInt(recycleChance()) == 0` (`recycleChance() == 8`).
* `BasicMachineRecipeManager.addRecipe` rejects `input == output` unless `metadata` contains `ignoreSameInputOutput`.
* `Ic2Items.advancedMachine == blockMachine:12`; `Ic2Items.machine == blockMachine:0`; `BlockMachine.damageDropped` returns 12 / meta depending on configuration.

## Appendix B — verification method

* IC2 classes were decompiled from the exact dependency jar (`industrialcraft-2-2.2.828-experimental-dev.jar`, build dated 2017-06-19) with CFR 0.152, including `TileEntityStandardMachine`, `TileEntityMacerator`, `TileEntityCompressor`, `TileEntityExtractor`, `TileEntityRecycler`, `TileEntityOreWashing`, `TileEntityElectricMachine`, `TileEntityInventory`, `InvSlot`, `InvSlotOutput`, `InvSlotProcessableGeneric`, `InvSlotUpgrade`, `ContainerBase`, `ContainerStandardMachine`, `ContainerOreWashing`, `ContainerElectricMachine`, `ItemUpgradeModule`, `BlockMultiID`, `BlockBase`, `BlockMachine`, `TileEntityBlock`, `ItemToolWrench`, `BasicMachineRecipeManager`.
* The current working tree was diffed (whitespace/format-insensitive) against the upstream original `AdvancedMachinesAS` sources at `Jorch72/atomicstrykers-minecraft-mods`; only formatting/reshuffled imports differ, so all findings are pre-existing mod bugs.
* Container slot numbering was derived from `ContainerFullInv` (36 player slots first) + `ContainerElectricMachine` (+1 discharge) + `ContainerStandardMachine` (+1 input, +1 output, +4 upgrades) + `ContainerAdvancedMachine` (+1/+2 extra outputs) + `ContainerAdvancedMacerator` (+1 supplement), i.e. supplement = container slot **44**.
* Sprite-table comparison was done programmatically over all 36 `(side, facing)` pairs.

---

## 10. Implementation progress (batches)

Changes are landed as small, tested batches. `./gradlew test --offline` is the gate for every batch.

| Batch | Scope | Status |
|---|---|---|
| 1 | Test harness (JUnit), `MachineMath` extraction, speed/math tests, config validation (§1.14) | ✅ done |
| 2 | Macerator supplement correctness (§1.1, §1.2, §1.10, §1.12) | ✅ done |
| 3 | Speed energy gating + redstone stall removal (§1.4, §1.6) | ✅ done |
| 4 | Container/block/index/drops robustness (§1.3, §1.8, §1.11, §1.13) | ✅ done |
| 5 | Recycler scrap chance + polish/localization/build metadata (§1.7, §4, §7, §1.15) | ✅ done |

### Batch 1 details (done)

* Added `src/test` JUnit 4 coverage (14 tests) via `testImplementation("junit:junit:4.13.2")`.
* Extracted the pure speed/energy math into `MachineMath` and unit tested it: the original
  integer-division ramp, its bounds/overflow cases, and every degenerate config combination.
  `CommonLogicAdvancedMachines` now delegates to it.
* Config validation (`ModAdvancedMachines.validateConfig`) repairs `maxMachineSpeedUpFactor < 2`,
  `maxMachineSpeedUpTicks <= 0` and non-positive/non-finite `machinePowerDrawFactor`, writing the
  corrected values back so the config self-heals. Resolves the validation half of §1.14.
* The new tests immediately caught two real clamp bugs in the new code (negative and
  `Integer.MAX_VALUE`-overflowing speed values), demonstrating the value of the harness.

### Batch 2 details (done)

* New `MaceratorSupplement` class holds the supplement decision table as a pure, null-safe function.
  A non-matching supplement now returns `Outcome.NONE` instead of an empty non-null list, so the
  caller can no longer mistake "nothing matched" for "replace the output with nothing". This is the
  fix for §1.1; `getSpecialResultFor` is also called only once per result now.
* `CommonLogicAdvancedMachines` exposes an `OutputStatus` (`NO_INPUT`, `NO_RECIPE`, `NOT_STORABLE`,
  `STALLED`, `READY`) so the macerator can tell "no base recipe" apart from "the result does not
  fit". `TileEntityAdvancedMacerator.getOutput()` only runs the supplement logic on a `READY` output
  (or on a genuine `NO_RECIPE` input for the supplement-only quartz/netherrack rules), fixing the
  §1.2 stash-check bypass.
* The global identity recipes and the stateful `IdentRecipe` hack were removed from
  `ModAdvancedMachines`; quartz ore and netherrack are handled entirely inside the advanced
  macerator, so plain IC2 macerators are no longer polluted (§1.12).
* Supplement state: `supplementedItemsLeft` is persisted to NBT, the extra input drain is applied
  through a null/negative-safe `consumeInput()` (clearing emptied slots), and changing the supplement
  item resets a stale charge. §1.10.
* Tests: 16 pure decision-table tests + 10 tests that drive the real tile entity against a throw-away
  `BasicMachineRecipeManager`, covering the output-preservation regression, the blocked-output case,
  the four supplement rules, the netherrack drain and the NBT round trip.

### Batch 3 details (done)

* Extracted the config values into `AdvancedMachineSettings` (with `DEFAULTS`) so the helper no longer
  reads `ModAdvancedMachines.instance` fields directly. `ModAdvancedMachines` builds the settings in
  `preInit` after `validateConfig()`.
* `CommonLogicAdvancedMachines.updateEntity(IProgressAccess, boolean ranThisTick)` now grants extra
  progress **only on ticks the base machine actually ran and paid EU for**. The old
  `runningEmpty`-based branch advanced (and banked) progress for an under-powered machine, letting it
  run at up to 10x speed on a fraction of the intended energy. §1.4.
* Deleted the fake dirt `RecipeOutput`/`runningEmpty` machinery. A redstone signal now simply idles
  the machine: `getOutput()` returns `null` with status `STALLED`, so the base resets progress, draws
  no EU and the speed ramp decays. This also removes the `consume without a processing result` crash
  and the dirt-output path entirely. §1.6.
* Tests: 5 new tests for the shared logic (extra progress only while running, no idle progress, ramp
  up/down, redstone stall, power-factor scaling).

### Batch 4 details (done)

* §1.3: deleted the `ContainerAdvancedMacerator.slotClick` override entirely. It compared the
  *container slot id* (8 = a player hotbar slot) against the tile-entity supplement index and could
  delete a player item; the charge state is already handled correctly by the tile entity
  (`operateOnce` clears it when the slot empties, and `resetChargeIfSupplementChanged` handles swaps).
* §1.8: `damageDropped` now returns IC2's advanced machine damage (12) when the wrench requirement is
  in effect, and `getItemDropped`/`damageDropped` mirror IC2's `balance/ignoreWrenchRequirement`
  behaviour when it is disabled.
* §1.11: the supplement slot is now `InvSlot.Access.NONE`, so automation can neither insert nor
  extract, while the player-managed plain container slot still works. The misleading
  `isItemValidForSlot`/`canExtractItem` overrides (which special-cased legacy `oldStartIndex` values as
  if they were inventory indexes) are gone; the extra output slot is handled correctly by the base
  `InvSlotOutput`.
* §1.13: `getIcon` is metadata-bounds checked and null-safe (corrupt meta no longer throws an
  `ArrayIndexOutOfBoundsException`), `isActive` tolerates a missing tile entity, `onBlockPlacedBy`
  only runs server-side and null-checks the tile entity/player, the dead `breakBlock` duplicating loop
  was removed, and `canCreatureSpawn` now returns false like IC2's machines.
* Tests: 5 new block tests (drop damage, meta bounds, spawn guard) plus 2 automation tests for the
  supplement/extra-output slot access rights.
* Ran `spotlessApply` over the tree.

### Batch 5 details (done)

* §1.7: `TileEntityAdvancedRecycler.operateOnce` now consumes the input and only inserts scrap on
  IC2's 1-in-8 roll (`MachineMath.rollScrap(IC2.random, recycleChance())`), instead of always
  inserting via the shared helper. Covered by a statistical test.
* §1.14/§2.6: after applying the power-draw factor, `setOverclockRates` grows `maxEnergy` so the
  buffer covers at least one boosted operation (the base sized it for the un-multiplied draw).
* §1.15/§4 polish:
  * `@Mod` now uses the generated `Tags.VERSION` and requires `IC2@2.2.828` (was hardcoded `1.1.6` /
    `2.2.775`).
  * `mcmod.info` no longer advertises the unused `UniMixins` dependency.
  * GUI labels are localised (`container.inventory` + new `advancedmachines.gui.*` keys in en_US,
    de_DE and ru_RU), the Rotary Ore Washer GUI gained the missing speed readout, and its fluid
    tooltip is translated and null-safe.
  * README documented (machines, supplement table, config, build/test commands).
* §7 leftovers fixed: removed the unused `MAIN_IN_SLOT_INDEX`, `ItemAdvancedMachine` no longer
  returns a null name for unknown metadata and uses the same creative tab as the block, the block's
  step sound matches IC2 metal machines, and the pointless try/catch in `readFromNBT` is gone.
* Still open (low priority): `ItemAdvancedMachine.getSubItems` does not filter config-disabled
  machines; the GUI constructors take an unused tile-entity parameter; the custom GUIs do not show
  IC2's charge/upgrade tooltips.

### Remaining low-priority items (not yet done)

Everything in the executive summary's Critical/High/Medium list is now addressed. These are the
deliberately deferred, low-impact leftovers:

* §1.13/§3.4: the block does not close open GUIs when broken (IC2's `onBlockPreDestroy` does). Stale
  client containers are harmless today but would be nice to close.
* §3.6: `onNeighborBlockChange` is not forwarded to `TileEntityBlock.onNeighborUpdate`; inert for
  these machines because they register no IC2 components.
* §2.6: `getInventoryName()`/`getGui()` still inherit the base machine names/screens, so WAILA/NEI
  show "Macerator" etc. and any direct `IHasGui` use would open the base GUI.
* §2.6: the >6-overclocker guard is still an explosion; an insert-time cap (rejecting the upgrade or
  limiting the slot stack size) would be more robust.
* §7: `ItemAdvancedMachine.getSubItems` does not filter config-disabled machines; the GUI constructors
  keep an unused tile-entity parameter; the custom GUIs omit IC2's charge/upgrade tooltips.
* §5: performance polish — cache the supplement recipe lookups per input, and avoid the double tile
  entity lookup in `BlockAdvancedMachines.getIcon`.
