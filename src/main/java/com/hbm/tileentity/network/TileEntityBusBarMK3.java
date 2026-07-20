package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energymk3.IEnergyConductorMK3;
import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityBusBarMK3 extends TileEntityLoadedBase implements IEnergyConductorMK3 {

	protected PowerNodeMK3 node;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(this.node == null || this.node.expired) {
				this.node = NodespaceMK3.getNode(worldObj, xCoord, yCoord, zCoord);
				if(this.node == null || this.node.expired) {
					this.node = this.createNode();
					this.node.setCableProps(VoltageTier.BUS, 0, Double.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
					NodespaceMK3.createNode(worldObj, this.node);
				}
			}
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(!worldObj.isRemote && this.node != null) {
			NodespaceMK3.destroyNode(worldObj, xCoord, yCoord, zCoord);
		}
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
	}

	@Override
	public VoltageTier getVoltageTier() { return VoltageTier.BUS; }
	@Override
	public double getResistance() { return 0; }
	@Override
	public double getMaxAmperage() { return Double.MAX_VALUE; }
	@Override
	public long getMaxThroughput() { return Long.MAX_VALUE; }
	@Override
	public long getInternalBuffer() { return Long.MAX_VALUE; }
}
