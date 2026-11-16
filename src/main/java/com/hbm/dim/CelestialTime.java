package com.hbm.dim;

import com.hbm.config.SpaceConfig;
import com.hbm.util.AstronomyUtil;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

// Rebases the celestial clock when the day length changes, planet local times are rescaled lazily in CelestialBodyWorldSavedData
public class CelestialTime extends WorldSavedData {

	private static final String DATA_NAME = "NTMCelestialTime";

	// <= 0 uses the config value
	public double dayLengthOverride = -1;
	public long timeOffset = 0;
	// 0 = saved before configurable day lengths, treat as 1
	public double lastMultiplier = 0;

	public CelestialTime(String name) {
		super(name);
	}

	public static CelestialTime get(World world) {
		CelestialTime result = (CelestialTime) world.mapStorage.loadData(CelestialTime.class, DATA_NAME);

		if(result == null) {
			result = new CelestialTime(DATA_NAME);
			world.mapStorage.setData(DATA_NAME, result);
		}

		return result;
	}

	public double getDayLengthMinutes() {
		return dayLengthOverride > 0 ? dayLengthOverride : SpaceConfig.dayLengthMinutes;
	}

	public static void update(World overworld) {
		if(overworld == null || overworld.isRemote) return;

		CelestialTime data = get(overworld);
		AstronomyUtil.DAY_LENGTH_FACTOR = data.getDayLengthMinutes() / 20.0D;

		double current = AstronomyUtil.getTimeMultiplier();
		if(data.lastMultiplier != current) {
			double previous = data.lastMultiplier > 0 ? data.lastMultiplier : 1.0D;
			double ratio = previous / current;

			// (T + offset) * multiplier must stay constant
			long totalTime = overworld.getTotalWorldTime();
			data.timeOffset = Math.round((totalTime + data.timeOffset) * ratio) - totalTime;

			overworld.provider.setWorldTime(Math.round(overworld.provider.getWorldTime() * ratio));

			data.lastMultiplier = current;
			data.markDirty();
		}

		AstronomyUtil.TIME_OFFSET = data.timeOffset;
	}

	public static void setDayLength(World overworld, double minutes) {
		CelestialTime data = get(overworld);
		data.dayLengthOverride = minutes;
		data.markDirty();
		update(overworld);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		dayLengthOverride = nbt.hasKey("dayLength") ? nbt.getDouble("dayLength") : -1;
		timeOffset = nbt.getLong("offset");
		lastMultiplier = nbt.getDouble("multiplier");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setDouble("dayLength", dayLengthOverride);
		nbt.setLong("offset", timeOffset);
		nbt.setDouble("multiplier", lastMultiplier);
	}

}
