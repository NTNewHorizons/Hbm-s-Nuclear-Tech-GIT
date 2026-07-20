package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CableCopper extends BlockCableMK3 {

	public CableCopper() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		TileEntityCableMK3 te = new TileEntityCableMK3();
		te.voltageTier = VoltageTier.LV;
		te.resistance = 0.01;
		te.maxAmperage = 8.3;
		te.maxPower = 1000;
		te.internalBuffer = 100;
		return te;
	}
}
