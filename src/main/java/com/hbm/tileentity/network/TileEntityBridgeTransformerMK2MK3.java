package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.energymk2.IEnergyProviderMK2;
import api.hbm.energymk3.IEnergyConductorMK3;
import api.hbm.energymk3.IEnergyProviderMK3;
import api.hbm.energymk3.IEnergyReceiverMK3;
import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityBridgeTransformerMK2MK3 extends TileEntityLoadedBase implements IEnergyReceiverMK2, IEnergyProviderMK2, IEnergyConductorMK3 {

	public long mk2Buffer;
	public long mk3Buffer;
	public long maxPower = 100000;
	public VoltageTier mk3VoltageTier = VoltageTier.MV;
	public long transferRate = 1000;

	protected PowerNodeMK3 node;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(this.node == null || this.node.expired) {
				this.node = NodespaceMK3.getNode(worldObj, xCoord, yCoord, zCoord);
				if(this.node == null || this.node.expired) {
					this.node = this.createNode();
					this.node.setCableProps(this.getVoltageTier(), 0, Double.MAX_VALUE, Long.MAX_VALUE, this.maxPower);
					NodespaceMK3.createNode(worldObj, this.node);
				}
			}

			if(mk2Buffer > 0 && mk3Buffer < maxPower) {
				long transfer = Math.min(mk2Buffer, Math.min(transferRate, maxPower - mk3Buffer));
				mk2Buffer -= transfer;
				mk3Buffer += transfer;
			}

			if(mk3Buffer > 0 && mk2Buffer < maxPower) {
				long transfer = Math.min(mk3Buffer, Math.min(transferRate, maxPower - mk2Buffer));
				mk3Buffer -= transfer;
				mk2Buffer += transfer;
			}

			this.networkPackNT(25);
		}
	}

	@Override public long transferPower(long power) {
		if(power + this.mk2Buffer <= this.maxPower) {
			this.mk2Buffer += power;
			return 0;
		}
		long capacity = this.maxPower - this.mk2Buffer;
		this.mk2Buffer = this.maxPower;
		return power - capacity;
	}

	@Override public long getPower() { return this.mk2Buffer; }
	@Override public void setPower(long power) { this.mk2Buffer = power; }
	@Override public long getMaxPower() { return this.maxPower; }
	@Override public VoltageTier getVoltageTier() { return this.mk3VoltageTier; }
	@Override public double getResistance() { return 0; }
	@Override public double getMaxAmperage() { return Double.MAX_VALUE; }
	@Override public long getMaxThroughput() { return Long.MAX_VALUE; }
	@Override public long getInternalBuffer() { return this.maxPower; }
	@Override public boolean canConnect(ForgeDirection dir) { return true; }

	@Override public PowerNodeMK3 createNode() { return IEnergyConductorMK3.super.createNode(); }

	@Override
	public void invalidate() {
		super.invalidate();
		if(!worldObj.isRemote && this.node != null) {
			NodespaceMK3.destroyNode(worldObj, xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void usePower(long power) {
		this.mk2Buffer = Math.max(0, this.mk2Buffer - power);
	}

	@Override
	public long getProviderSpeed() {
		return this.transferRate;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.mk2Buffer = nbt.getLong("mk2Buffer");
		this.mk3Buffer = nbt.getLong("mk3Buffer");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("mk2Buffer", mk2Buffer);
		nbt.setLong("mk3Buffer", mk3Buffer);
	}
}
