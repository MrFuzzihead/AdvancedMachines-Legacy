package atomicstryker.ic2.advancedmachines;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import ic2.api.item.IC2Items;
import ic2.api.tile.IWrenchable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.BlockBase;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.init.MainConfig;
import ic2.core.item.tool.ItemToolWrench;
import ic2.core.util.ConfigUtil;

public class BlockAdvancedMachines extends BlockContainer {

    /** Number of machine variants (one per block metadata value). */
    public static final int MACHINE_COUNT = 5;

    /** IC2 advanced machine casing item damage (`Ic2Items.advancedMachine`). */
    private static final int ADVANCED_MACHINE_DAMAGE = 12;

    /** Bound for the before-break tile entity stash (same order of magnitude as IC2's). */
    private static final int MAX_TES_BEFORE_BREAK = 8;

    /**
     * Tile entities captured in {@link #onBlockPreDestroy} just before removal. Vanilla removes the
     * tile entity before calling {@code getDrops()}, so without this the machine's contents were lost
     * when mined. IC2's BlockMultiID uses the same trick.
     */
    private static final ArrayDeque<TileEntity> TES_BEFORE_BREAK = new ArrayDeque<TileEntity>(MAX_TES_BEFORE_BREAK);

    private IIcon[][] iconBuffer;

    public BlockAdvancedMachines() {
        super(Material.iron);
        this.setHardness(2.0F);
        this.setStepSound(soundTypeMetal);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[MACHINE_COUNT][12]; // one machine per meta, 6 sides each, ON and OFF

        // meta 0, macerator
        // first the 6 sides in OFF state
        iconBuffer[0][0] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[0][1] = par1IconRegister.registerIcon("advancedmachines:topMaceratorOFF"); // top
        iconBuffer[0][2] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[0][3] = par1IconRegister.registerIcon("advancedmachines:frontMaceratorOFF"); // east
        iconBuffer[0][4] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[0][5] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west
        // then the 6 sides in ON state
        iconBuffer[0][6] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[0][7] = par1IconRegister.registerIcon("advancedmachines:topMaceratorON"); // top
        iconBuffer[0][8] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[0][9] = par1IconRegister.registerIcon("advancedmachines:frontMaceratorON"); // east
        iconBuffer[0][10] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[0][11] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west

        // meta 1, compressor
        // first the 6 sides in OFF state
        iconBuffer[1][0] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[1][1] = par1IconRegister.registerIcon("advancedmachines:topCompressorOFF"); // top
        iconBuffer[1][2] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[1][3] = par1IconRegister.registerIcon("advancedmachines:frontCompressorOFF"); // east
        iconBuffer[1][4] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[1][5] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west
        // then the 6 sides in ON state
        iconBuffer[1][6] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[1][7] = par1IconRegister.registerIcon("advancedmachines:topCompressorON"); // top
        iconBuffer[1][8] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[1][9] = par1IconRegister.registerIcon("advancedmachines:frontCompressorON"); // east
        iconBuffer[1][10] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[1][11] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west

        // meta 2, extractor
        // first the 6 sides in OFF state
        iconBuffer[2][0] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[2][1] = par1IconRegister.registerIcon("advancedmachines:topExtractorOFF"); // top
        iconBuffer[2][2] = par1IconRegister.registerIcon("advancedmachines:sideExtractorOFF"); // north
        iconBuffer[2][3] = par1IconRegister.registerIcon("advancedmachines:frontExtractorOFF"); // east
        iconBuffer[2][4] = par1IconRegister.registerIcon("advancedmachines:sideExtractorOFF"); // south
        iconBuffer[2][5] = par1IconRegister.registerIcon("advancedmachines:sideExtractorOFF"); // west
        // then the 6 sides in ON state
        iconBuffer[2][6] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[2][7] = par1IconRegister.registerIcon("advancedmachines:topExtractorON"); // top
        iconBuffer[2][8] = par1IconRegister.registerIcon("advancedmachines:sideExtractorON"); // north
        iconBuffer[2][9] = par1IconRegister.registerIcon("advancedmachines:frontExtractorON"); // east
        iconBuffer[2][10] = par1IconRegister.registerIcon("advancedmachines:sideExtractorON"); // south
        iconBuffer[2][11] = par1IconRegister.registerIcon("advancedmachines:sideExtractorON"); // west

        // meta 3, recycler
        // first the 6 sides in OFF state
        iconBuffer[3][0] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[3][1] = par1IconRegister.registerIcon("advancedmachines:topMaceratorOFF"); // top
        iconBuffer[3][2] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[3][3] = par1IconRegister.registerIcon("advancedmachines:frontRecyclerOFF"); // east
        iconBuffer[3][4] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[3][5] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west
        // then the 6 sides in ON state
        iconBuffer[3][6] = par1IconRegister.registerIcon("advancedmachines:bottom"); // bottom
        iconBuffer[3][7] = par1IconRegister.registerIcon("advancedmachines:topMaceratorON"); // top
        iconBuffer[3][8] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // north
        iconBuffer[3][9] = par1IconRegister.registerIcon("advancedmachines:frontRecyclerON"); // east
        iconBuffer[3][10] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // south
        iconBuffer[3][11] = par1IconRegister.registerIcon("advancedmachines:sideplate"); // west

        // meta 4, orewasher
        // first the 6 sides in OFF state
        iconBuffer[4][0] = par1IconRegister.registerIcon("advancedmachines:bottom32"); // bottom
        iconBuffer[4][1] = par1IconRegister.registerIcon("advancedmachines:topOreWasher"); // top
        iconBuffer[4][2] = par1IconRegister.registerIcon("advancedmachines:sideOreWasherOFF"); // north
        iconBuffer[4][3] = par1IconRegister.registerIcon("advancedmachines:frontOreWasherOFF"); // east
        iconBuffer[4][4] = par1IconRegister.registerIcon("advancedmachines:sideOreWasherOFF"); // south
        iconBuffer[4][5] = par1IconRegister.registerIcon("advancedmachines:back32"); // west
        // then the 6 sides in ON state
        iconBuffer[4][6] = par1IconRegister.registerIcon("advancedmachines:bottom32"); // bottom
        iconBuffer[4][7] = par1IconRegister.registerIcon("advancedmachines:topOreWasher"); // top
        iconBuffer[4][8] = par1IconRegister.registerIcon("advancedmachines:sideOreWasherOFF"); // north
        iconBuffer[4][9] = par1IconRegister.registerIcon("advancedmachines:frontOreWasherOFF"); // east
        iconBuffer[4][10] = par1IconRegister.registerIcon("advancedmachines:sideOreWasherOFF"); // south
        iconBuffer[4][11] = par1IconRegister.registerIcon("advancedmachines:back32"); // west
    }

    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int blockSide) {
        int blockMeta = world.getBlockMetadata(x, y, z);
        if (iconBuffer == null || !supportsMeta(blockMeta)) {
            return null;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        int facing = (te instanceof TileEntityBlock) ? ((int) (((TileEntityBlock) te).getFacing())) : 0;

        // IC2's own face mapping, indexed [facing][side]. The +6 selects the running (ON) texture set.
        int subIndex = BlockBase.getTextureSubIndex(facing, blockSide);
        boolean active = te instanceof TileEntityBlock && ((TileEntityBlock) te).getActive();
        if (active) {
            subIndex += 6;
        }
        return iconBuffer[blockMeta][subIndex];
    }

    @Override
    public IIcon getIcon(int blockSide, int blockMeta) {
        if (iconBuffer == null || !supportsMeta(blockMeta)) {
            return null;
        }
        // Inventory rendering: IC2's default facing is 3.
        return iconBuffer[blockMeta][BlockBase.getTextureSubIndex(3, blockSide)];
    }

    /** Guards against corrupt/foreign metadata instead of throwing an ArrayIndexOutOfBoundsException. */
    static boolean supportsMeta(int meta) {
        return meta >= 0 && meta < MACHINE_COUNT;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        switch (meta) {
            case 0:
                return new TileEntityAdvancedMacerator();
            case 1:
                return new TileEntityAdvancedCompressor();
            case 2:
                return new TileEntityAdvancedExtractor();
            case 3:
                return new TileEntityAdvancedRecycler();
            case 4:
                return new TileEntityAdvancedOreWasher();
            default:
                return null;
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> dropList = super.getDrops(world, x, y, z, metadata, fortune);
        IInventory inventory = findInventory(world, x, y, z);
        if (inventory != null) {
            for (int index = 0; index < inventory.getSizeInventory(); ++index) {
                ItemStack itemstack = inventory.getStackInSlot(index);
                if (itemstack != null) {
                    dropList.add(itemstack);
                    inventory.setInventorySlotContents(index, null);
                }
            }
        }

        return dropList;
    }

    private static IInventory findInventory(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IInventory) {
            return (IInventory) te;
        }
        Iterator<TileEntity> iterator = TES_BEFORE_BREAK.descendingIterator();
        while (iterator.hasNext()) {
            TileEntity candidate = iterator.next();
            if (candidate.getWorldObj() == world && candidate.xCoord == x
                && candidate.yCoord == y
                && candidate.zCoord == z) {
                iterator.remove();
                return candidate instanceof IInventory ? (IInventory) candidate : null;
            }
        }
        return null;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        if (ignoreWrenchRequirement()) {
            return Item.getItemFromBlock(this);
        }
        ItemStack advancedMachine = IC2Items.getItem("advancedMachine");
        return advancedMachine != null ? advancedMachine.getItem() : Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(int meta) {
        return droppedDamage(meta, ignoreWrenchRequirement());
    }

    /**
     * The item that vanilla creates for a broken machine is
     * {@code new ItemStack(getItemDropped(meta), 1, damageDropped(meta))}. Without the override below
     * the damage defaulted to 0, i.e. mining yielded IC2's basic machine casing instead of the
     * advanced machine component the code intended.
     */
    static int droppedDamage(int meta, boolean ignoreWrenchRequirement) {
        return ignoreWrenchRequirement ? meta : ADVANCED_MACHINE_DAMAGE;
    }

    /**
     * Mirrors IC2's {@code BlockMachine} behaviour: by default the machine itself is only recovered
     * with a wrench, while mining returns the advanced machine casing.
     */
    private static boolean ignoreWrenchRequirement() {
        try {
            return ConfigUtil.getBool(MainConfig.get(), "balance/ignoreWrenchRequirement");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Get the block's damage value (for use with pick block).
     */
    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z); // advanced machine item meta
                                                // exactly equals the block meta
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        if (!IC2.platform.isSimulating()) {
            return;
        }
        super.onBlockPlacedBy(world, x, y, z, player, stack);

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityBlock)) {
            return;
        }
        TileEntityBlock teb = (TileEntityBlock) te;

        if (player == null) {
            teb.setFacing((short) 2);
            return;
        }

        int heading = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        switch (heading) {
            case 0:
                teb.setFacing((short) 2);
                break;
            case 1:
                teb.setFacing((short) 5);
                break;
            case 2:
                teb.setFacing((short) 3);
                break;
            case 3:
                teb.setFacing((short) 4);
                break;
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int par6, float par7,
        float par8, float par9) {
        // A wrench is handled by ItemToolWrench#onItemUseFirst. The client still reaches this method
        // (its onItemUseFirst returns false outside the simulation), which used to pop the GUI open
        // while the machine was being rotated or removed.
        if (entityPlayer.isSneaking() || isHoldingWrench(entityPlayer)) {
            return false;
        }
        entityPlayer.openGui(ModAdvancedMachines.instance, 0, world, x, y, z);
        return true;
    }

    private static boolean isHoldingWrench(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemToolWrench;
    }

    private boolean isActive(IBlockAccess iba, int x, int y, int z) {
        TileEntity te = iba.getTileEntity(x, y, z);
        return te instanceof TileEntityBlock && ((TileEntityBlock) te).getActive();
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityBlock) {
            ((TileEntityBlock) te).onNeighborUpdate(neighbor);
        }
    }

    /**
     * Closes any open GUI for this machine before it is destroyed, mirroring IC2's BlockMultiID.
     * Without this a player could keep interacting with a stale container after the block is removed.
     */
    @Override
    public void onBlockPreDestroy(World world, int x, int y, int z, int meta) {
        super.onBlockPreDestroy(world, x, y, z, meta);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) {
            return;
        }

        // Stash the inventory so getDrops() can still read it after BlockContainer removes the TE.
        if (te instanceof IInventory) {
            if (TES_BEFORE_BREAK.size() >= MAX_TES_BEFORE_BREAK) {
                TES_BEFORE_BREAK.pollLast();
            }
            TES_BEFORE_BREAK.push(te);
        }

        if (!(te instanceof IHasGui)) {
            return;
        }
        for (Object obj : world.playerEntities) {
            if (!(obj instanceof EntityPlayerMP)) {
                continue;
            }
            EntityPlayerMP player = (EntityPlayerMP) obj;
            if (player.openContainer instanceof ContainerBase && ((ContainerBase<?>) player.openContainer).base == te) {
                player.closeScreen();
            }
        }
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        // Drop any stale stash entry for this position (e.g. a wrench drop followed by a new block).
        Iterator<TileEntity> iterator = TES_BEFORE_BREAK.descendingIterator();
        while (iterator.hasNext()) {
            TileEntity candidate = iterator.next();
            if (candidate.getWorldObj() == world && candidate.xCoord == x
                && candidate.yCoord == y
                && candidate.zCoord == z) {
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) {
        if (axis == ForgeDirection.UNKNOWN) {
            return false;
        }
        TileEntity tileEntity = worldObj.getTileEntity(x, y, z);

        if ((tileEntity instanceof IWrenchable)) {
            IWrenchable te = (IWrenchable) tileEntity;

            int newFacing = ForgeDirection.getOrientation(te.getFacing())
                .getRotation(axis)
                .ordinal();

            if (te.wrenchCanSetFacing(null, newFacing)) {
                te.setFacing((short) newFacing);
            }
        }

        return false;
    }

    @Override
    public void randomDisplayTick(World world, int i, int j, int k, Random random) {
        if (!IC2.platform.isRendering()) {
            return;
        }
        int meta = world.getBlockMetadata(i, j, k);

        if ((meta == 1) && (isActive(world, i, j, k))) {
            TileEntity te = world.getTileEntity(i, j, k);
            int facing = (te instanceof TileEntityBlock) ? ((TileEntityBlock) te).getFacing() : 0;

            float f = i + 0.5F;
            float f1 = j + 0.0F + random.nextFloat() * 6.0F / 16.0F;
            float f2 = k + 0.5F;
            float f3 = 0.52F;
            float f4 = random.nextFloat() * 0.6F - 0.3F;

            switch (facing) {
                case 4:
                    world.spawnParticle("smoke", f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                    world.spawnParticle("flame", f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                    break;
                case 5:
                    world.spawnParticle("smoke", f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                    world.spawnParticle("flame", f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                    break;
                case 2:
                    world.spawnParticle("smoke", f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
                    world.spawnParticle("flame", f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
                    break;
                case 3:
                    world.spawnParticle("smoke", f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
                    world.spawnParticle("flame", f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
            }

        }
        if ((meta == 3) && (isActive(world, i, j, k))) {
            float f = i + 1.0F;
            float f1 = j + 1.0F;
            float f2 = k + 1.0F;
            for (int z = 0; z < 4; z++) {
                float fmod = -0.2F - random.nextFloat() * 0.6F;
                float f1mod = -0.1F + random.nextFloat() * 0.2F;
                float f2mod = -0.2F - random.nextFloat() * 0.6F;
                world.spawnParticle("smoke", f + fmod, f1 + f1mod, f2 + f2mod, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World par1World, int par2, int par3, int par4, int par5) {
        TileEntity te = par1World.getTileEntity(par2, par3, par4);
        if (te != null) {
            if ((te instanceof TileEntityStandardMachine)) {
                TileEntityStandardMachine tem = (TileEntityStandardMachine) te;
                return (int) Math.floor(tem.getProgress() * 15.0F);
            }
        }

        return 0;
    }

}
