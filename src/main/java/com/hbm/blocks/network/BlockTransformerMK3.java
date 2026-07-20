package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityTransformerMK3;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockTransformerMK3 extends BlockContainer {

	public BlockTransformerMK3() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityTransformerMK3();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		int l = MathHelper.floor_double(player.rotationYaw * 4.0 / 360.0 + 0.5) & 3;
		if(l == 0) world.setBlockMetadataWithNotify(x, y, z, 2, 2);
		if(l == 1) world.setBlockMetadataWithNotify(x, y, z, 5, 2);
		if(l == 2) world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		if(l == 3) world.setBlockMetadataWithNotify(x, y, z, 4, 2);
	}

	@Override
	public boolean isOpaqueCube() { return false; }
	@Override
	public boolean renderAsNormalBlock() { return false; }
}
