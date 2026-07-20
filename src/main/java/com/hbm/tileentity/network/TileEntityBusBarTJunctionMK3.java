package com.hbm.tileentity.network;

import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityBusBarTJunctionMK3 extends TileEntityBusBarMK3 {

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir == ForgeDirection.NORTH || dir == ForgeDirection.EAST || dir == ForgeDirection.WEST;
	}
}
