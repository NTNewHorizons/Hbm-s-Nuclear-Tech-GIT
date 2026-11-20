package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats.MoxerStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMoxer;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineMoxer extends BlockDummyable implements ITooltipProvider {

	public MachineMoxer() {
		super(Material.rock);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMoxer();
		if(meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
		return null;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return this.standardOpenBehavior(world, x, y, z, player, 0);
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 1, 1, 2, 2};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block b, int i) {

		TileEntity te = world.getTileEntity(x, y, z);

		if(te instanceof TileEntityMoxer) {
			TileEntityMoxer crucible = (TileEntityMoxer) te;

			List<MoxerStack> stacks = new ArrayList();
			stacks.addAll(crucible.inpStack);

			for(MoxerStack stack : stacks) {
				ItemStack scrap = ItemScraps.create(new MaterialStack(stack.material, stack.amount));
				EntityItem item = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, scrap);
				world.spawnEntityInWorld(item);
			}

			crucible.inpStack.clear();
		}

		super.breakBlock(world, x, y, z, b, i);
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ + 1);
		this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ - 1);
		this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ + 1);
		this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ - 1);
		this.makeExtra(world, x - dir.offsetX + rot.offsetX * 2, y, z - dir.offsetZ + rot.offsetZ * 2);
		this.makeExtra(world, x - dir.offsetX - rot.offsetX * 2, y, z - dir.offsetZ - rot.offsetZ * 2);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		this.addStandardInfo(stack, player, list, ext);
	}
}
