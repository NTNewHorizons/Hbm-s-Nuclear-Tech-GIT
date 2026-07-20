package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CableSuperconductor extends BlockCableMK3 {

	public CableSuperconductor() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		TileEntityCableMK3 te = new TileEntityCableMK3();
		te.voltageTier = VoltageTier.SC;
		te.resistance = 0;
		te.maxAmperage = 3255;
		te.maxPower = 100000000;
		te.internalBuffer = 100000;
		return te;
	}
}
