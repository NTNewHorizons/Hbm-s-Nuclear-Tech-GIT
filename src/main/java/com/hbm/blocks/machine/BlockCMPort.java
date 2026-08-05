package com.hbm.blocks.machine;

import api.hbm.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.tileentity.TileEntityProxyCombo;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockCMPort extends BlockCM implements ITileEntityProvider, ICrucibleAcceptor {

	public BlockCMPort(Material mat, Class<? extends Enum> theEnum, boolean multiName, boolean multiTexture) {
		super(mat, theEnum, multiName, multiTexture);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityProxyCombo().inventory().power().fluid();
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block b, int m) {
		super.breakBlock(world, x, y, z, b, m);
		world.removeTileEntity(x, y, z);
	}

	@Override
	public boolean canAcceptPartialPour(World world, int x, int y, int z, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof ICrucibleAcceptor) {
			return ((ICrucibleAcceptor) te).canAcceptPartialPour(world, x, y, z, dX, dY, dZ, side, stack);
		}
		return false;
	}

	@Override
	public MaterialStack pour(World world, int x, int y, int z, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof ICrucibleAcceptor) {
			return ((ICrucibleAcceptor) te).pour(world, x, y, z, dX, dY, dZ, side, stack);
		}
		return stack;
	}

	@Override
	public boolean canAcceptPartialFlow(World world, int x, int y, int z, ForgeDirection side, MaterialStack stack) {
		return false;
	}

	@Override
	public MaterialStack flow(World world, int x, int y, int z, ForgeDirection side, MaterialStack stack) {
		return stack;
	}
}
