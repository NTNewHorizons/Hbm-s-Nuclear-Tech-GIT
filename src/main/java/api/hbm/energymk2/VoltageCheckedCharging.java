package api.hbm.energymk2;

import com.hbm.items.ModItems;
import com.hbm.lib.Library;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public final class VoltageCheckedCharging {

	private VoltageCheckedCharging() { }

	public static long chargeTEFromItems(IEnergyReceiverMK2 machine, ItemStack[] slots, int index, long power, long maxPower) {

		ItemStack stack = slots[index];

		if(stack != null && stack.getItem() instanceof IBatteryItem && stack.getItem() != ModItems.battery_creative) {

			long machineVoltage = getMachineBlockVoltage(machine);

			if(VoltageTier.isConfigured(machineVoltage)) {
				long batteryVoltage = BatteryVoltageRegistry.getVoltage(stack, VoltageTier.DEFAULT);

				// Legacy batteries have no registered tier and keep working as before,
				// only a registered, mismatching tier is treated as an overvoltage.
				if(VoltageTier.isConfigured(batteryVoltage) && batteryVoltage != machineVoltage) {
					machine.onOvervoltage(batteryVoltage);
					return power;
				}
			}
		}

		return Library.chargeTEFromItems(slots, index, power, maxPower);
	}

	public static long chargeItemsFromTE(IEnergyProviderMK2 machine, ItemStack[] slots, int index, long power, long maxPower) {

		ItemStack stack = slots[index];

		if(stack != null && stack.getItem() instanceof IBatteryItem && stack.getItem() != ModItems.battery_creative) {

			long itemVoltage = BatteryVoltageRegistry.getVoltage(stack, VoltageTier.DEFAULT);

			long machineVoltage = getMachineBlockVoltage(machine);
			if(VoltageTier.isConfigured(itemVoltage) && VoltageTier.isConfigured(machineVoltage) && itemVoltage != machineVoltage) {
				return power;
			}
		}

		return Library.chargeItemsFromTE(slots, index, power, maxPower);
	}

	private static long getMachineBlockVoltage(Object machine) {
		if(machine instanceof TileEntity) {
			TileEntity te = (TileEntity) machine;
			Block block = te.getWorldObj() == null ? null : te.getBlockType();
			return MachineVoltageRegistry.getVoltageForBlock(block, VoltageTier.DEFAULT);
		}
		return VoltageTier.DEFAULT;
	}
}
