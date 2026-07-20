package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPylonSmallMK3;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PylonSmallMK3 extends PylonBaseMK3 {

	public PylonSmallMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityPylonSmallMK3();
	}
}
