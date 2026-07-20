package com.hbm.tileentity.network;

import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityCableFuseMK3 extends TileEntityCableMK3 {

	public boolean isFused = false;
	public int heat = 0;

	@Override
	public boolean canOverloadMelt() {
		return false;
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(!this.isFused) {
				super.updateEntity();
				if(this.node != null && this.node.hasValidNet()) {
					Double cableHeat = this.node.net.cableHeat.get(this.node);
					if(cableHeat != null && cableHeat > 1.0) {
						this.isFused = true;
						if(this.node != null) {
							NodespaceMK3.destroyNode(worldObj, xCoord, yCoord, zCoord);
							this.node = null;
						}
					}
				}
			} else {
				if(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
					this.isFused = false;
					this.heat = 0;
				}
			}
		}
	}

	@Override
	public boolean shouldCreateNode() {
		return !this.isFused;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("isFused", isFused);
		nbt.setInteger("heat", heat);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.isFused = nbt.getBoolean("isFused");
		this.heat = nbt.getInteger("heat");
	}
}
