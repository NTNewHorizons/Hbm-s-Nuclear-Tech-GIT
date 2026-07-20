package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.network.TileEntityPylonBaseMK3;
import com.hbm.tileentity.network.TileEntityPylonMediumMK3;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PylonMediumMK3 extends BlockDummyable {

	protected boolean hasTransformer;

	public PylonMediumMK3(boolean hasTransformer) {
		super(Material.iron);
		this.hasTransformer = hasTransformer;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) {
			TileEntityPylonMediumMK3 te = new TileEntityPylonMediumMK3();
			te.setHasTransformer(this.hasTransformer);
			return te;
		}
		return null;
	}

	@Override
	public int[] getDimensions() { return new int[] {6, 0, 0, 0, 0, 0}; }
	@Override
	public int getOffset() { return 0; }

	@Override
	public void breakBlock(World world, int x, int y, int z, Block b, int m) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityPylonBaseMK3) ((TileEntityPylonBaseMK3) te).disconnectAll();
		super.breakBlock(world, x, y, z, b, m);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;
		if(!player.isSneaking()) {
			int[] pos = this.findCore(world, x, y, z);
			if(pos != null) {
				TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
				if(te instanceof TileEntityPylonBaseMK3) return ((TileEntityPylonBaseMK3) te).setColor(player.getHeldItem());
			}
		}
		return false;
	}
}
