package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityBusBarTJunctionMK3;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockBusBarTJunctionMK3 extends BlockBusBarMK3 {

	public BlockBusBarTJunctionMK3() {
		super();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityBusBarTJunctionMK3();
	}
}
