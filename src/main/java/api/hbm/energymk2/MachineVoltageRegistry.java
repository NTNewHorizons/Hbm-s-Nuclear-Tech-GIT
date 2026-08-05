package api.hbm.energymk2;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public final class MachineVoltageRegistry {

	private static final Map<Block, Long> BLOCK_VOLTAGES = new HashMap<Block, Long>();
	private static final Map<Block, Float> BLOCK_EXPLOSION_STRENGTH = new HashMap<Block, Float>();
	private static boolean configLoaded;

	private MachineVoltageRegistry() { }

	private static synchronized void ensureConfigLoaded() {
		if(!configLoaded) configLoaded = MachineVoltageConfig.registerAll();
	}

	public static void setVoltage(Block block, long voltage) {
		setVoltage(block, voltage, VoltageTier.DEFAULT_EXPLOSION_STRENGTH);
	}

	public static void setVoltage(Block block, long voltage, float explosionStrength) {
		if(block == null) throw new IllegalArgumentException("Block cannot be null");
		if(voltage <= 0) throw new IllegalArgumentException("Voltage must be positive");
		BLOCK_VOLTAGES.put(block, voltage);
		BLOCK_EXPLOSION_STRENGTH.put(block, explosionStrength);
	}

	public static long getVoltageForBlock(Block block, long fallback) {
		ensureConfigLoaded();
		Long voltage = block == null ? null : BLOCK_VOLTAGES.get(block);
		return voltage == null ? fallback : voltage.longValue();
	}

	public static long getVoltage(Object tile) {
		ensureConfigLoaded();
		if(tile instanceof TileEntity) {
			TileEntity te = (TileEntity) tile;
			Block block = te.getWorldObj() == null ? null : te.getBlockType();
			Long voltage = block == null ? null : BLOCK_VOLTAGES.get(block);
			if(voltage != null) return voltage.longValue();
		}
		return VoltageTier.DEFAULT;
	}

	public static float getExplosionStrengthForBlock(Block block, float fallback) {
		ensureConfigLoaded();
		Float strength = block == null ? null : BLOCK_EXPLOSION_STRENGTH.get(block);
		return strength == null ? fallback : strength.floatValue();
	}

	public static float getExplosionStrength(Object tile) {
		ensureConfigLoaded();
		if(tile instanceof TileEntity) {
			TileEntity te = (TileEntity) tile;
			Block block = te.getWorldObj() == null ? null : te.getBlockType();
			Float strength = block == null ? null : BLOCK_EXPLOSION_STRENGTH.get(block);
			if(strength != null) return strength.floatValue();
		}
		return VoltageTier.DEFAULT_EXPLOSION_STRENGTH;
	}
}
