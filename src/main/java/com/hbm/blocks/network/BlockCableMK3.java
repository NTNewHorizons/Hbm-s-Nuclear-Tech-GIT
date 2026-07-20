package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.tileentity.network.TileEntityCableMK3;

import api.hbm.energymk3.IEnergyConnectorBlockMK3;
import api.hbm.energymk3.IEnergyConnectorMK3;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class BlockCableMK3 extends BlockContainer {

	public BlockCableMK3(Material mat) {
		super(mat);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	public static boolean canConnectMK3(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		if(y > 255 || y < 0) return false;

		net.minecraft.block.Block b = world.getBlock(x, y, z);
		if(b instanceof IEnergyConnectorBlockMK3) {
			IEnergyConnectorBlockMK3 con = (IEnergyConnectorBlockMK3) b;
			if(con.canConnect(world, x, y, z, dir.getOpposite())) return true;
		}

		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof IEnergyConnectorMK3) {
			IEnergyConnectorMK3 con = (IEnergyConnectorMK3) te;
			if(con.canConnect(dir.getOpposite())) return true;
		}

		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		boolean posX = canConnectMK3(world, x + 1, y, z, Library.POS_X);
		boolean negX = canConnectMK3(world, x - 1, y, z, Library.NEG_X);
		boolean posY = canConnectMK3(world, x, y + 1, z, Library.POS_Y);
		boolean negY = canConnectMK3(world, x, y - 1, z, Library.NEG_Y);
		boolean posZ = canConnectMK3(world, x, y, z + 1, Library.POS_Z);
		boolean negZ = canConnectMK3(world, x, y, z - 1, Library.NEG_Z);

		setBounds(posX, negX, posY, negY, posZ, negZ);

		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		boolean posX = canConnectMK3(world, x + 1, y, z, Library.POS_X);
		boolean negX = canConnectMK3(world, x - 1, y, z, Library.NEG_X);
		boolean posY = canConnectMK3(world, x, y + 1, z, Library.POS_Y);
		boolean negY = canConnectMK3(world, x, y - 1, z, Library.NEG_Y);
		boolean posZ = canConnectMK3(world, x, y, z + 1, Library.POS_Z);
		boolean negZ = canConnectMK3(world, x, y, z - 1, Library.NEG_Z);

		setBounds(posX, negX, posY, negY, posZ, negZ);
	}

	private void setBounds(boolean posX, boolean negX, boolean posY, boolean negY, boolean posZ, boolean negZ) {
		float pixel = 0.0625F;
		float min = pixel * 5.5F;
		float max = pixel * 10.5F;

		float minX = negX ? 0F : min;
		float maxX = posX ? 1F : max;
		float minY = negY ? 0F : min;
		float maxY = posY ? 1F : max;
		float minZ = negZ ? 0F : min;
		float maxZ = posZ ? 1F : max;

		this.setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
