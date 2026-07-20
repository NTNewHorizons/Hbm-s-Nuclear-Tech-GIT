package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineElectricFurnaceMK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineElectricFurnaceMK3 extends BlockContainer {

	public MachineElectricFurnaceMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityMachineElectricFurnaceMK3();
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
