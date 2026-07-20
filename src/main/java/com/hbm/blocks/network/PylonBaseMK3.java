package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPylonBaseMK3;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class PylonBaseMK3 extends BlockContainer {

	protected PylonBaseMK3(Material mat) {
		super(mat);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block b, int m) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityPylonBaseMK3) ((TileEntityPylonBaseMK3) te).disconnectAll();
		super.breakBlock(world, x, y, z, b, m);
	}

	@Override
	public int getRenderType() { return -1; }
	@Override
	public boolean isOpaqueCube() { return false; }
	@Override
	public boolean renderAsNormalBlock() { return false; }

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;
		if(!player.isSneaking()) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityPylonBaseMK3) {
				return ((TileEntityPylonBaseMK3) te).setColor(player.getHeldItem());
			}
		}
		return false;
	}
}
