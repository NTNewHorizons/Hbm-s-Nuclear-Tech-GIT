package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CableGold extends BlockCableMK3 {

	public CableGold() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		TileEntityCableMK3 te = new TileEntityCableMK3();
		te.voltageTier = VoltageTier.MV;
		te.resistance = 0.005;
		te.maxAmperage = 20.8;
		te.maxPower = 10000;
		te.internalBuffer = 500;
		return te;
	}
}
