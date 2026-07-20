package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityBusBarMK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockBusBarMK3 extends BlockContainer {

	public BlockBusBarMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityBusBarMK3();
	}

	@Override
	public boolean isOpaqueCube() { return true; }
	@Override
	public boolean renderAsNormalBlock() { return true; }
}
