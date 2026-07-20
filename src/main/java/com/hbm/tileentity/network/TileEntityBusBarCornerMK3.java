package com.hbm.tileentity.network;

import api.hbm.energymk3.IEnergyConductorMK3;
import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityBusBarCornerMK3 extends TileEntityBusBarMK3 {

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir == ForgeDirection.NORTH || dir == ForgeDirection.EAST;
	}
}
