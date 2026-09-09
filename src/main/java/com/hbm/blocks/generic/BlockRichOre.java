package com.hbm.blocks.generic;

import java.util.ArrayList;
import java.util.List;

import com.hbm.items.ModItems;
import com.hbm.render.block.RenderBlockMultipass;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Depletable ore (iron/copper): metadata is richness, final unit turns to stone.
 * Extends BlockOre only for rendering.
 */
public class BlockRichOre extends BlockOre {

	public enum RichOreType {
		IRON,
		COPPER
	}

	// units in a fresh block
	public static final int MAX_UNITS = 8;

	public final RichOreType type;

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
	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
		if(world.isRemote) return;

		int units = OreRichnessHelper.getUnitsRemaining(world, x, y, z);

		if(units > 0) {
			ItemStack chunk = getChunkStack();
			chunk.stackSize = Math.min(units, chunk.getMaxStackSize());
			this.dropBlockAsItem(world, x, y, z, chunk);
		}
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		int meta = Math.max(0, Math.min(world.getBlockMetadata(x, y, z), MAX_UNITS - 1));
		return new ItemStack(this, 1, meta);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		int meta = Math.max(0, Math.min(stack.getItemDamage(), MAX_UNITS - 1));
		world.setBlockMetadataWithNotify(x, y, z, meta, 2);
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

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		int units = Math.max(1, Math.min(stack.getItemDamage() + 1, MAX_UNITS));
		list.add(EnumChatFormatting.GOLD + "" + units + " / " + MAX_UNITS + " units");
	}
}
