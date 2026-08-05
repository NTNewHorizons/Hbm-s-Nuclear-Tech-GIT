package api.hbm.energymk2;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm.config.GeneralConfig;
import com.hbm.main.MainRegistry;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/**
 * Central gate for how strictly the voltage tier system is enforced.
 *
 * The legacy MK2 grid explicitly allowed any machine to connect to any wire, pylon and battery,
 * so mixed-tier networks were perfectly legal in older versions. Turning on the new rules
 * unconditionally would therefore detonate every pre-existing base on update.
 *
 * Three modes bridge the gap:
 *  legacy - everything behaves exactly as it did before the voltage system. No checks, no
 *           explosions, mismatched tiers still transfer power. This is the default and keeps
 *           existing worlds fully functional.
 *  warn   - power still flows (legacy compatible), but mismatched connections and battery
 *           insertions are reported to nearby players so networks can be upgraded first.
 *  strict - the full new system: mismatched tiers refuse power, overvolted machines and
 *           cables explode, and wrong-tier batteries are refused.
 */
public final class VoltageEnforcement {

	public enum Mode {
		LEGACY,
		WARN,
		STRICT
	}

	private static Mode currentMode = Mode.LEGACY;

	/** Prevents warning spam. Tiles are tracked weakly so they can be garbage collected. */
	private static final Map<TileEntity, Long> LAST_WARNED = new WeakHashMap<TileEntity, Long>();
	private static final long WARN_COOLDOWN_MS = 5_000L;

	private VoltageEnforcement() { }

	public static void setMode(String raw) {
		if("strict".equalsIgnoreCase(raw)) {
			currentMode = Mode.STRICT;
		} else if("warn".equalsIgnoreCase(raw)) {
			currentMode = Mode.WARN;
		} else {
			currentMode = Mode.LEGACY;
		}
	}

	/** Whether the voltage system is switched on at all (see the 1.46_enableVoltageSystem config entry). */
	public static boolean isEnabled() { return GeneralConfig.enableVoltageSystem; }

	public static Mode getMode() { return currentMode; }
	/** True when the system is disabled entirely or explicitly set to legacy mode. */
	public static boolean isLegacy() { return !GeneralConfig.enableVoltageSystem || currentMode == Mode.LEGACY; }
	public static boolean isWarn() { return GeneralConfig.enableVoltageSystem && currentMode == Mode.WARN; }
	public static boolean isStrict() { return GeneralConfig.enableVoltageSystem && currentMode == Mode.STRICT; }

	/**
	 * Whether a mismatched connection may be denied. In legacy and warn modes the power must
	 * keep flowing so old builds are not silently starved.
	 */
	public static boolean shouldDenyTransfer() { return isStrict(); }

	/**
	 * Handles a voltage mismatch for a tile according to the current mode.
	 * legacy: no-op. warn: notifies nearby players (throttled). strict: nothing here, the
	 * caller is expected to trigger its own punishment (explosion / refusal).
	 */
	public static void handleMismatch(TileEntity tile, String messageKey, Object... args) {
		if(isLegacy()) return;
		if(tile == null || tile.getWorldObj() == null || tile.getWorldObj().isRemote) return;
		warnNearby(tile, messageKey, args);
	}

	/** Sends a throttled warning to players near the given tile and logs it to the console. */
	public static void warnNearby(TileEntity tile, String messageKey, Object... args) {
		World world = tile.getWorldObj();
		if(world == null || world.isRemote) return;

		long now = System.currentTimeMillis();
		Long last = LAST_WARNED.get(tile);
		if(last != null && now - last.longValue() < WARN_COOLDOWN_MS) return;
		LAST_WARNED.put(tile, Long.valueOf(now));

		MainRegistry.logger.warn("Voltage mismatch at {} [{} {} {}]: {}",
				new Object[] { world.provider.getDimensionName(), tile.xCoord, tile.yCoord, tile.zCoord, messageKey });

		double x = tile.xCoord + 0.5D;
		double y = tile.yCoord + 0.5D;
		double z = tile.zCoord + 0.5D;

		for(Object o : world.playerEntities) {
			if(!(o instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP) o;
			double dx = player.posX - x;
			double dy = player.posY - y;
			double dz = player.posZ - z;
			if(dx * dx + dy * dy + dz * dz > 32D * 32D) continue;
			ChatComponentTranslation message = new ChatComponentTranslation(messageKey, args);
			message.getChatStyle().setColor(EnumChatFormatting.YELLOW);
			player.addChatMessage(message);
		}
	}

	/** For debugging or automated testing: reset the warning throttle state. */
	public static void resetWarnThrottle() {
		LAST_WARNED.clear();
	}

	public static String currentModeName() {
		if(!GeneralConfig.enableVoltageSystem) return "off";
		return GeneralConfig.voltageEnforcement;
	}
}
