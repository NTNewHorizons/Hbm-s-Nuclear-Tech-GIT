package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityBridgeTransformerMK2MK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockBridgeTransformerMK2MK3 extends BlockContainer {

	public BlockBridgeTransformerMK2MK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityBridgeTransformerMK2MK3();
	}

	@Override
	public boolean isOpaqueCube() { return false; }
	@Override
	public boolean renderAsNormalBlock() { return false; }
}
