package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineCentrifugeMK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineCentrifugeMK3 extends BlockContainer {

	public MachineCentrifugeMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityMachineCentrifugeMK3();
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
}
