package com.hbm.tileentity.machine;

import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk3.IEnergyProviderMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileEntityMK3Provider extends TileEntityMachineBase implements IEnergyProviderMK3 {

	public long power;
	public long maxPower;
	public long voltageNominal;
	public double voltageTolerance = 0.2;

	public TileEntityMK3Provider(int slotCount) {
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
		return 1.0;
	}

	@Override
	public VoltageTier getVoltageTier() {
		return VoltageTier.fromVoltage(this.voltageNominal);
	}

	public void updateConnections() {
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			this.tryProvide(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
	}
}
