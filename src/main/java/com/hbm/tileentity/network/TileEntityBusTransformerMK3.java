package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energymk3.IEnergyConductorMK3;
import api.hbm.energymk3.IEnergyReceiverMK3;
import api.hbm.energymk3.IEnergyProviderMK3;
import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityBusTransformerMK3 extends TileEntityLoadedBase implements IEnergyConductorMK3, IEnergyReceiverMK3, IEnergyProviderMK3 {

	public VoltageTier outputTier = VoltageTier.HV;
	public long throughputLimit = 100000;
	public long buffer;
	public long bufferCapacity = Long.MAX_VALUE / 1000;

	protected PowerNodeMK3 node;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(this.node == null || this.node.expired) {
				this.node = NodespaceMK3.getNode(worldObj, xCoord, yCoord, zCoord);
				if(this.node == null || this.node.expired) {
					this.node = this.createNode();
					this.node.setCableProps(VoltageTier.BUS, 0, Double.MAX_VALUE, Long.MAX_VALUE, this.bufferCapacity);
					NodespaceMK3.createNode(worldObj, this.node);
				}
			}

			if(this.buffer > 0) {
				ForgeDirection outputDir = this.getOutputDirection();
				this.tryProvide(worldObj, xCoord + outputDir.offsetX, yCoord + outputDir.offsetY, zCoord + outputDir.offsetZ, outputDir);
			}
		}
	}

	private ForgeDirection getOutputDirection() {
		int meta = this.getBlockMetadata();
		if(meta == 2) return ForgeDirection.NORTH;
		if(meta == 3) return ForgeDirection.SOUTH;
		if(meta == 4) return ForgeDirection.WEST;
		if(meta == 5) return ForgeDirection.EAST;
		return ForgeDirection.NORTH;
	}

	@Override public long transferPower(long power) {
		if(power + this.buffer <= this.bufferCapacity) {
			this.buffer += power;
			return 0;
		}
		long capacity = this.bufferCapacity - this.buffer;
		this.buffer = this.bufferCapacity;
		return power - capacity;
	}

	@Override public long getPower() { return Math.min(this.buffer, this.throughputLimit); }
	@Override public void setPower(long power) { this.buffer = power; }
	@Override public long getMaxPower() { return this.bufferCapacity; }
	@Override public long getVoltageNominal() { return this.outputTier.getVoltage(); }
	@Override public double getVoltageTolerance() { return 1.0; }
	@Override public double getEfficiency() { return 1.0; }
	@Override public VoltageTier getVoltageTier() { return VoltageTier.BUS; }
	@Override public double getResistance() { return 0; }
	@Override public double getMaxAmperage() { return Double.MAX_VALUE; }
	@Override public long getMaxThroughput() { return this.throughputLimit; }
	@Override public long getInternalBuffer() { return this.bufferCapacity; }
	@Override public long getProviderSpeed() { return Math.min(this.buffer, this.throughputLimit); }

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN && dir != getOutputDirection();
	}

	@Override public PowerNodeMK3 createNode() { return IEnergyConductorMK3.super.createNode(); }

	@Override
	public void invalidate() {
		super.invalidate();
		if(!worldObj.isRemote && this.node != null) NodespaceMK3.destroyNode(worldObj, xCoord, yCoord, zCoord);
	}

	@Override public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.buffer = nbt.getLong("buffer");
		this.outputTier = VoltageTier.values()[nbt.getInteger("outputTier")];
		this.throughputLimit = nbt.getLong("throughputLimit");
	}

	@Override public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("buffer", buffer);
		nbt.setInteger("outputTier", outputTier.ordinal());
		nbt.setLong("throughputLimit", throughputLimit);
	}
}
