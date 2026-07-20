package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineCombustionEngineMK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineCombustionEngineMK3 extends BlockContainer {

	public MachineCombustionEngineMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityMachineCombustionEngineMK3();
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
