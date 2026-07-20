package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableFuseMK3;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockCableFuseMK3 extends BlockCableMK3 {

	public BlockCableFuseMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityCableFuseMK3();
	}
}
