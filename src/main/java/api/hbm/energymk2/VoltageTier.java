package api.hbm.energymk2;

import java.text.NumberFormat;
import java.util.Locale;

import net.minecraft.util.StatCollector;

public final class VoltageTier {

	public static final long LV = 32L;
	public static final long MV = 128L;
	public static final long HV = 512L;
	public static final long EV = 2048L;
	public static final long IV = 8192L;
	public static final long LUV = 32768L;
	public static final long ZPM = 131072L;
	public static final long UV = 524288L;
	public static final long UHV = 2097152L;
	public static final long UEV = 8388608L;
	public static final long UIV = 33554432L;
	public static final long UMV = 134217728L;
	public static final long UXV = 536870912L;
	public static final long UNKNOWN = 2147483648L;

	public static final long DEFAULT = 0L;
	public static final float DEFAULT_EXPLOSION_STRENGTH = 4.0F;

	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

	private VoltageTier() { }

	public static boolean isConfigured(long voltage) {
		return voltage > 0L;
	}

	public static String getTierName(long voltage) {
		if(voltage == LV) return "LV";
		if(voltage == MV) return "MV";
		if(voltage == HV) return "HV";
		if(voltage == EV) return "EV";
		if(voltage == IV) return "IV";
		if(voltage == LUV) return "LUV";
		if(voltage == ZPM) return "ZPM";
		if(voltage == UV) return "UV";
		if(voltage == UHV) return "UHV";
		if(voltage == UEV) return "UEV";
		if(voltage == UIV) return "UIV";
		if(voltage == UMV) return "UMV";
		if(voltage == UXV) return "UXV";
		if(voltage == UNKNOWN) return "UNKNOWN";
		return null;
	}

	public static String format(long voltage) {
		if(!isConfigured(voltage)) return StatCollector.translateToLocal("hbm.voltage.unconfigured");

		String number = NUMBER_FORMAT.format(voltage);
		String tierName = getTierName(voltage);

		return tierName != null ? number + " V [" + tierName + "]" : number + " V";
	}
}
