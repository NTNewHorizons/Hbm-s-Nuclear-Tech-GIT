package com.hbm.blocks.generic;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.items.ModItems;
import com.hbm.render.block.RenderBlockMultipass;
import com.hbm.saveddata.RichOreData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * Depletable ore (iron/copper): metadata is richness, final unit turns to stone.
 * Extends BlockOre only for rendering.
 */
public class BlockRichOre extends BlockOre implements ILookOverlay {

	public enum RichOreType {
		IRON,
		COPPER
	}

	// units in a fresh block
	public static final int MAX_UNITS = 8;

	public final RichOreType type;

	public static int clampMeta(int meta) {
		return Math.max(0, Math.min(meta, MAX_UNITS - 1));
	}

	public BlockRichOre(RichOreType type) {
		super(Material.rock);
		this.type = type;
		this.setHardness(type == RichOreType.IRON ? 3.0F : 5.0F);
		this.setResistance(type == RichOreType.IRON ? 5.0F : 10.0F);
		this.setStepSound(soundTypeStone);
	}

	public ItemStack getChunkStack() {
		if(type == RichOreType.IRON) return new ItemStack(ModItems.chunk_rich_iron);
		return new ItemStack(ModItems.chunk_rich_copper);
	}

	@Override
	public int getMobilityFlag() {
		return 2;
	}

	// metadata is richness, not a planet variant: background is always plain stone
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		if(RenderBlockMultipass.currentPass == 0) {
			return stoneIcons[0];
		}

		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		if(RenderBlockMultipass.currentPass == 0) {
			return stoneIcons[0];
		}

		return blockIcon;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
		return false;
	}

	@Override
	public Item getItemDropped(int meta, java.util.Random rand, int fortune) {
		return getChunkStack().getItem();
	}

	@Override
	public int quantityDropped(java.util.Random rand) {
		return 1;
	}

	@Override
	public int quantityDroppedWithBonus(int fortune, java.util.Random rand) {
		if(!allowFortune) return 1;
		return OreRichnessHelper.rollChunkCount(rand, fortune);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
		ItemStack chunk = getChunkStack();
		chunk.stackSize = allowFortune ? OreRichnessHelper.rollChunkCount(world.rand, fortune) : 1;
		drops.add(chunk);
		return drops;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		if(world.isRemote) return super.removedByPlayer(world, player, x, y, z, willHarvest);

		if(player.capabilities.isCreativeMode) {
			world.setBlockToAir(x, y, z);
			return true;
		}

		// nothing drops, nothing depletes
		if(!willHarvest) return false;

		OreRichnessHelper.consumeOneUnit(world, x, y, z);
		this.harvestBlock(world, player, x, y, z, world.getBlockMetadata(x, y, z));

		return false;
	}

	// explosions release all remaining units at once
	// explosions pay every remaining unit; the generic chance-drop path is disabled below
	@Override
	public boolean canDropFromExplosion(Explosion explosion) {
		return false;
	}

	@Override
	public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
		if(!world.isRemote) {
			int units = OreRichnessHelper.getUnitsRemaining(world, x, y, z);

			while(units > 0) {
				int n = Math.min(units, 64);
				ItemStack chunk = getChunkStack();
				chunk.stackSize = n;
				this.dropBlockAsItem(world, x, y, z, chunk);
				units -= n;
			}

			RichOreData.forWorld(world).remove(x, y, z);
		}

		super.onBlockExploded(world, x, y, z, explosion);
	}

	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
		if(!world.isRemote) RichOreData.forWorld(world).remove(x, y, z);
	}

	// any non-drain removal drops its map entry, so reused coords start clean
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if(!world.isRemote) RichOreData.forWorld(world).remove(x, y, z);
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		return new ItemStack(this, 1, clampMeta(world.getBlockMetadata(x, y, z)));
	}

	// placement is always a fresh full block; the block is unobtainable in survival anyway
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		if(!world.isRemote) RichOreData.forWorld(world).remove(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, MAX_UNITS - 1, 2);
	}

	@Override
	public int getSubCount() {
		return MAX_UNITS;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item, 1, MAX_UNITS - 1));
	}

	// tiers by stage: 5-7 rich, 2-4 half-depleted, 0-1 almost depleted
	@Override
	public String getOverrideDisplayName(ItemStack stack) {
		int meta = clampMeta(stack.getItemDamage());
		if(meta >= 5) return null;

		String base = "tile." + (type == RichOreType.IRON ? "ore_rich_iron" : "ore_rich_copper");
		return StatCollector.translateToLocal(base + (meta >= 2 ? ".half.name" : ".low.name")).trim();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(RenderGameOverlayEvent.Pre event, World world, int x, int y, int z) {
		int meta = clampMeta(world.getBlockMetadata(x, y, z));
		String title = getOverrideDisplayName(new ItemStack(this, 1, meta));
		if(title == null) title = StatCollector.translateToLocal(this.getUnlocalizedName() + ".name");

		List<String> text = new ArrayList<String>();
		text.add(((meta + 1) * 100 / MAX_UNITS) + "%");

		ILookOverlay.printGeneric(event, title, 0xffff00, 0x404000, text);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		int meta = clampMeta(stack.getItemDamage());
		list.add(EnumChatFormatting.GOLD + "" + ((meta + 1) * 100 / MAX_UNITS) + "%");
	}
}
