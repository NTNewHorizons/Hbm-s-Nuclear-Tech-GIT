package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineChemDispenser;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineChemDispenser extends BlockMachineBase {

	public MachineChemDispenser(Material material) {
		super(material, 0);
		this.rotatable = true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileEntityMachineChemDispenser();
	}
}
