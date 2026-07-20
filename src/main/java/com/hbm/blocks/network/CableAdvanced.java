package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CableAdvanced extends BlockCableMK3 {

	public CableAdvanced() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		TileEntityCableMK3 te = new TileEntityCableMK3();
		te.voltageTier = VoltageTier.EV;
		te.resistance = 0.0005;
		te.maxAmperage = 1302;
		te.maxPower = 10000000;
		te.internalBuffer = 10000;
		return te;
	}
}
