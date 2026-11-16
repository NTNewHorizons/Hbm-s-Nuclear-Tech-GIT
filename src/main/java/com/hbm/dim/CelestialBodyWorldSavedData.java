package com.hbm.dim;

import com.hbm.util.AstronomyUtil;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class CelestialBodyWorldSavedData extends WorldSavedData {

	private static final String DATA_NAME = "CelestialBodyData";

	public CelestialBodyWorldSavedData(String name) {
		super(name);
	}

	private WorldProviderCelestial provider;
	private long localTime;
	private double timeMultiplier; // 0 = saved before configurable day lengths, treat as 1

	private boolean requiresLoad;
	private NBTTagCompound nbt;
	
	public static CelestialBodyWorldSavedData get(WorldProviderCelestial provider) {
		CelestialBodyWorldSavedData result = (CelestialBodyWorldSavedData) provider.worldObj.perWorldStorage.loadData(CelestialBodyWorldSavedData.class, DATA_NAME);
		
		if(result == null) {
			provider.worldObj.perWorldStorage.setData(DATA_NAME, new CelestialBodyWorldSavedData(DATA_NAME));
			result = (CelestialBodyWorldSavedData) provider.worldObj.perWorldStorage.loadData(CelestialBodyWorldSavedData.class, DATA_NAME);
		}

		// readFromNBT is called before the result is returned and a WorldProvider is made available, so we call this here
		if(result.requiresLoad) {
			result.requiresLoad = false;
			provider.readFromNBT(result.nbt);
		}

		result.provider = provider;
		
		return result;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		localTime = nbt.getLong("time");
		timeMultiplier = nbt.getDouble("timeMult");
		this.nbt = nbt;
		requiresLoad = true;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("time", localTime);
		nbt.setDouble("timeMult", timeMultiplier);
		provider.writeToNBT(nbt);
	}

	public long getLocalTime() {
		rescaleIfNeeded();
		return localTime;
	}

	public void setLocalTime(long time) {
		rescaleIfNeeded();
		localTime = time;
		markDirty();
	}

	private void rescaleIfNeeded() {
		double current = AstronomyUtil.getTimeMultiplier();
		if(timeMultiplier == current) return;

		double previous = timeMultiplier > 0 ? timeMultiplier : 1.0D;
		localTime = Math.round(localTime * (previous / current));
		timeMultiplier = current;
		markDirty();
	}
	
}
