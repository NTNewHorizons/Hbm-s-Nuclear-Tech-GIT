package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityBusBarCornerMK3;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockBusBarCornerMK3 extends BlockBusBarMK3 {

	public BlockBusBarCornerMK3() {
		super();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityBusBarCornerMK3();
	}
}
