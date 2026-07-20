package com.hbm.tileentity.machine;

import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk3.IEnergyReceiverMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileEntityMK3Consumer extends TileEntityMachineBase implements IEnergyReceiverMK3 {

	public long power;
	public long maxPower;
	public long voltageNominal;
	public double voltageTolerance = 0.2;
	public long lastReceivedVoltage;

	public TileEntityMK3Consumer(int slotCount) {
		super(slotCount);
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getPower() {
		return this.power;
	}

	@Override
	public long getMaxPower() {
		return this.maxPower;
	}

	@Override
	public long getVoltageNominal() {
		return this.voltageNominal;
	}

	@Override
	public double getVoltageTolerance() {
		return this.voltageTolerance;
	}

	@Override
	public double getEfficiency() {
		if(this.lastReceivedVoltage <= 0) return 1.0;
		long vMin = (long) (this.voltageNominal * (1 - this.voltageTolerance));
		if(this.lastReceivedVoltage < vMin) {
			return Math.max(0.1, (double) this.lastReceivedVoltage / (double) this.voltageNominal);
		}
		return 1.0;
	}

	@Override
	public VoltageTier getVoltageTier() {
		return VoltageTier.fromVoltage(this.voltageNominal);
	}

	@Override
	public void onUndervoltage(long voltageReceived, long voltageNominal) {
		this.lastReceivedVoltage = voltageReceived;
	}

	@Override
	public void onOvervoltage(long voltageReceived, long voltageNominal) {
		this.lastReceivedVoltage = voltageReceived;
		if(!worldObj.isRemote) {
			long vMax = (long) (voltageNominal * (1 + this.voltageTolerance));
			long excess = voltageReceived - vMax;
			if(excess > 0) {
				double damageChance = (double) excess / (double) vMax;
				if(worldObj.rand.nextDouble() < damageChance) {
					int radius = (int) Math.ceil((double) excess / (double) voltageNominal);
					worldObj.createExplosion(null, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, radius, true);
				}
			}
		}
	}

	public void updateConnections() {
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
	}
}
