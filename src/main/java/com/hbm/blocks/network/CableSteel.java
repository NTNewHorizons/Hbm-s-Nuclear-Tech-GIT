package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CableSteel extends BlockCableMK3 {

	public CableSteel() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		TileEntityCableMK3 te = new TileEntityCableMK3();
		te.voltageTier = VoltageTier.HV;
		te.resistance = 0.002;
		te.maxAmperage = 52;
		te.maxPower = 100000;
		te.internalBuffer = 2000;
		return te;
	}
}
