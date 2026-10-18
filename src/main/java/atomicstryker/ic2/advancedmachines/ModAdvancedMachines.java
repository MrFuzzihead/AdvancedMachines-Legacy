package atomicstryker.ic2.advancedmachines;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

@Mod(
    modid = "AdvancedMachines",
    name = "IC2 Advanced Machines Addon",
    version = Tags.VERSION,
    dependencies = "required-after:IC2@2.2.828")
public class ModAdvancedMachines implements IGuiHandler, IProxy {

    @SidedProxy(
        clientSide = "atomicstryker.ic2.advancedmachines.client.AdvancedMachinesClient",
        serverSide = "atomicstryker.ic2.advancedmachines.ModAdvancedMachines")
    public static IProxy proxy;

    @Instance(value = "AdvancedMachines")
    public static ModAdvancedMachines instance;

    private Configuration config;

    private Block blockAdvancedMachine;
    private ItemStack stackRotaryMacerator;
    private ItemStack stackSingularityCompressor;
    private ItemStack stackCentrifugeExtractor;
    private ItemStack stackCombinedRecycler;
    private ItemStack stackRotaryOreWasher;

    public int maxMachineSpeedUpFactor;
    public int maxMachineSpeedUpTicks;
    public double machinePowerDrawFactor;

    private AdvancedMachineSettings settings = AdvancedMachineSettings.DEFAULTS;

    private static final String KEY_MAX_SPEED_UP_FACTOR = "maxMachineSpeedUpFactor";
    private static final String KEY_MAX_SPEED_UP_TICKS = "maxMachineSpeedUpTicks";
    private static final String KEY_MACHINE_POWER_DRAW_FACTOR = "machinePowerDrawFactor";

    private static final String[] MACHINE_ENABLED_KEYS = { "Rotary Macerator Enabled", "Singularity Compressor Enabled",
        "Centrifuge Extractor Enabled", "Combined Recycler Enabled", "Rotary Ore Washer Enabled" };

    private final boolean[] machineEnabled = new boolean[] { true, true, true, true, true };

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        config = new Configuration(evt.getSuggestedConfigurationFile());
        config.load();

        blockAdvancedMachine = new BlockAdvancedMachines();
        blockAdvancedMachine.setCreativeTab(CreativeTabs.tabRedstone);

        GameRegistry.registerBlock(blockAdvancedMachine, ItemAdvancedMachine.class, "blockAdvMachine");

        stackRotaryMacerator = new ItemStack(blockAdvancedMachine, 1, 0);
        stackSingularityCompressor = new ItemStack(blockAdvancedMachine, 1, 1);
        stackCentrifugeExtractor = new ItemStack(blockAdvancedMachine, 1, 2);
        stackCombinedRecycler = new ItemStack(blockAdvancedMachine, 1, 3);
        stackRotaryOreWasher = new ItemStack(blockAdvancedMachine, 1, 4);

        maxMachineSpeedUpFactor = config
            .get(
                Configuration.CATEGORY_GENERAL,
                KEY_MAX_SPEED_UP_FACTOR,
                AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR,
                "Advanced Machines will reach X times the speed of normal machines")
            .getInt(AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR);
        maxMachineSpeedUpTicks = config
            .get(
                Configuration.CATEGORY_GENERAL,
                KEY_MAX_SPEED_UP_TICKS,
                AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS,
                "Advanced Machines will take X ingame ticks to reach max speed")
            .getInt(AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS);
        machinePowerDrawFactor = config
            .get(
                Configuration.CATEGORY_GENERAL,
                KEY_MACHINE_POWER_DRAW_FACTOR,
                AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR,
                "Advanced Machines will draw X times the normal machines power")
            .getDouble(AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR);

        validateConfig();
        settings = new AdvancedMachineSettings(maxMachineSpeedUpFactor, maxMachineSpeedUpTicks, machinePowerDrawFactor);

        for (int meta = 0; meta < machineEnabled.length; meta++) {
            machineEnabled[meta] = config.get(Configuration.CATEGORY_GENERAL, MACHINE_ENABLED_KEYS[meta], true)
                .getBoolean(true);
        }
    }

    public AdvancedMachineSettings getSettings() {
        return settings;
    }

    /** @return whether the machine for {@code meta} is enabled in the config (defaults to true). */
    public boolean isMachineEnabled(int meta) {
        return meta < 0 || meta >= machineEnabled.length || machineEnabled[meta];
    }

    /** Test seam for {@link #isMachineEnabled(int)}. */
    void setMachineEnabled(int meta, boolean enabled) {
        if (meta >= 0 && meta < machineEnabled.length) {
            machineEnabled[meta] = enabled;
        }
    }

    /**
     * Repairs out-of-range config values instead of letting them break the speed formula (division by
     * zero) or energy accounting (zero/negative power draw). Invalid entries are rewritten to the
     * defaults so the file self-heals.
     */
    private void validateConfig() {
        if (!MachineMath.isValidSpeedConfig(maxMachineSpeedUpFactor, maxMachineSpeedUpTicks)) {
            System.out.println(
                "[AdvancedMachines] Invalid speed config (maxMachineSpeedUpFactor=" + maxMachineSpeedUpFactor
                    + ", maxMachineSpeedUpTicks="
                    + maxMachineSpeedUpTicks
                    + "); falling back to "
                    + AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR
                    + "x over "
                    + AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS
                    + " ticks.");
            maxMachineSpeedUpFactor = AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR;
            maxMachineSpeedUpTicks = AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS;
            config
                .get(
                    Configuration.CATEGORY_GENERAL,
                    KEY_MAX_SPEED_UP_FACTOR,
                    AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR)
                .set(AdvancedMachineSettings.DEFAULT_SPEED_UP_FACTOR);
            config
                .get(
                    Configuration.CATEGORY_GENERAL,
                    KEY_MAX_SPEED_UP_TICKS,
                    AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS)
                .set(AdvancedMachineSettings.DEFAULT_SPEED_UP_TICKS);
        }
        if (!MachineMath.isValidPowerDrawFactor(machinePowerDrawFactor)) {
            System.out.println(
                "[AdvancedMachines] Invalid machinePowerDrawFactor (" + machinePowerDrawFactor
                    + "); falling back to "
                    + AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR
                    + ".");
            machinePowerDrawFactor = AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR;
            config
                .get(
                    Configuration.CATEGORY_GENERAL,
                    KEY_MACHINE_POWER_DRAW_FACTOR,
                    AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR)
                .set(AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR);
        }
    }

    @EventHandler
    public void load(FMLInitializationEvent evt) {
        GameRegistry.registerTileEntity(TileEntityAdvancedMacerator.class, "Rotary Macerator");
        GameRegistry.registerTileEntity(TileEntityAdvancedCompressor.class, "Singularity Compressor");
        GameRegistry.registerTileEntity(TileEntityAdvancedExtractor.class, "Centrifuge Extractor");
        GameRegistry.registerTileEntity(TileEntityAdvancedRecycler.class, "Combined Recycler");
        GameRegistry.registerTileEntity(TileEntityAdvancedOreWasher.class, "Rotary Ore Washer");

        NetworkRegistry.INSTANCE.registerGuiHandler(this, this);

        proxy.load();
    }

    @EventHandler
    public void afterModsLoaded(FMLPostInitializationEvent evt) {
        if (machineEnabled[0]) {
            GameRegistry.addRecipe(
                stackRotaryMacerator,
                new Object[] { "CRC", "PMP", "PAP", Character.valueOf('C'), IC2Items.getItem("coil"),
                    Character.valueOf('R'), IC2Items.getItem("elemotor"), Character.valueOf('P'),
                    IC2Items.getItem("plateiron"), Character.valueOf('M'), IC2Items.getItem("macerator"),
                    Character.valueOf('A'), IC2Items.getItem("advancedMachine") });
        }

        // The supplement-specific inputs (quartz ore, netherrack) are handled directly by
        // TileEntityAdvancedMacerator#getOutput; registering identity recipes here used to add
        // "X -> X" recipes to the global Recipes.macerator and therefore also affected plain IC2
        // macerators (they would waste power outputting the input unchanged).

        if (machineEnabled[1]) {
            GameRegistry.addRecipe(
                stackSingularityCompressor,
                new Object[] { "RGR", "MMM", "PAP", Character.valueOf('R'), Blocks.obsidian, Character.valueOf('G'),
                    IC2Items.getItem("reinforcedGlass"), Character.valueOf('P'), IC2Items.getItem("plateiron"),
                    Character.valueOf('M'), IC2Items.getItem("compressor"), Character.valueOf('A'),
                    IC2Items.getItem("advancedMachine") });
        }

        if (machineEnabled[2]) {
            GameRegistry.addRecipe(
                stackCentrifugeExtractor,
                new Object[] { "CEC", "RMR", "PAP", Character.valueOf('C'), IC2Items.getItem("coil"),
                    Character.valueOf('E'), IC2Items.getItem("elemotor"), Character.valueOf('R'),
                    IC2Items.getItem("electrolyzedWaterCell"), Character.valueOf('P'), IC2Items.getItem("plateiron"),
                    Character.valueOf('M'), IC2Items.getItem("extractor"), Character.valueOf('A'),
                    IC2Items.getItem("advancedMachine") });
        }

        if (machineEnabled[3]) {
            GameRegistry.addRecipe(
                stackCombinedRecycler,
                new Object[] { " M ", "PEP", "PRP", Character.valueOf('M'), stackRotaryMacerator,
                    Character.valueOf('E'), stackCentrifugeExtractor, Character.valueOf('P'),
                    IC2Items.getItem("plateiron"), Character.valueOf('R'), IC2Items.getItem("recycler") });
        }

        if (machineEnabled[4]) {
            GameRegistry.addRecipe(
                stackRotaryOreWasher,
                new Object[] { "CRC", "PWP", "PAP", Character.valueOf('C'), IC2Items.getItem("coil"),
                    Character.valueOf('R'), IC2Items.getItem("elemotor"), Character.valueOf('P'),
                    IC2Items.getItem("plateiron"), Character.valueOf('W'), IC2Items.getItem("orewashingplant"),
                    Character.valueOf('A'), IC2Items.getItem("advancedMachine") });
        }

        config.save();
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntityStandardMachine tesm = (TileEntityStandardMachine) world.getTileEntity(x, y, z);
        if (tesm != null) {
            if (tesm instanceof TileEntityAdvancedMacerator) {
                return new ContainerAdvancedMacerator(player, (TileEntityAdvancedMacerator) tesm);
            } else if (tesm instanceof TileEntityAdvancedOreWasher) {
                return new ContainerAdvancedOreWasher(player, (TileEntityAdvancedOreWasher) tesm);
            }

            return new ContainerAdvancedMachine<TileEntityStandardMachine>(player, tesm);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return proxy.getGuiElementForClient(ID, player, world, x, y, z);
    }

    @Override
    public void load() {
        // NOOP
    }

    @Override
    public Object getGuiElementForClient(int ID, EntityPlayer player, World world, int x, int y, int z) {
        // NOOP
        return null;
    }

}
