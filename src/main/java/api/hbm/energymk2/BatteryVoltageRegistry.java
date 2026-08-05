package api.hbm.energymk2;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class BatteryVoltageRegistry {

	private static final Map<Item, Map<Integer, Long>> ITEM_VOLTAGES = new HashMap<Item, Map<Integer, Long>>();

	private BatteryVoltageRegistry() { }

	public static void setVoltage(Item item, long voltage) {
		setVoltage(item, 0, voltage);
	}

	public static void setVoltage(Item item, int damage, long voltage) {
		if(item == null) throw new IllegalArgumentException("Item cannot be null");
		if(voltage <= 0) throw new IllegalArgumentException("Voltage must be positive");
		Map<Integer, Long> byDamage = ITEM_VOLTAGES.get(item);
		if(byDamage == null) {
			byDamage = new HashMap<Integer, Long>();
			ITEM_VOLTAGES.put(item, byDamage);
		}
		byDamage.put(damage, voltage);
	}

	public static long getVoltage(ItemStack stack, long fallback) {
		if(stack == null || stack.getItem() == null) return fallback;
		Map<Integer, Long> byDamage = ITEM_VOLTAGES.get(stack.getItem());
		if(byDamage == null) return fallback;
		Long voltage = byDamage.get(stack.getItemDamage());
		return voltage == null ? fallback : voltage.longValue();
	}
}
