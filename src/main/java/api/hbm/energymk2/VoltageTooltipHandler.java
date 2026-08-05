package api.hbm.energymk2;

import java.lang.reflect.Field;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.util.StatCollector;

public class VoltageTooltipHandler {

	private static final Field BLOCK_FIELD;
	static {
		Field field = null;
		try {
			field = ItemBlock.class.getDeclaredField("field_150939_a");
			field.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
		BLOCK_FIELD = field;
	}

	@SubscribeEvent
	public void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.itemStack;
		if(stack == null) return;

		Item item = stack.getItem();

		if(item instanceof ItemBlock && BLOCK_FIELD != null) {
			addBlockVoltageLine(stack, (ItemBlock) item, event.toolTip);
			return;
		}

		if(item instanceof IBatteryItem) {
			addBatteryVoltageLine(stack, event.toolTip);
		}
	}

	private void addBlockVoltageLine(ItemStack stack, ItemBlock itemBlock, List list) {
		Block block = getBlock(itemBlock);
		if(block == null) return;
		if(!MachineVoltageConfig.shouldShowVoltageTooltip(block)) return;

		long voltage = MachineVoltageRegistry.getVoltageForBlock(block, VoltageTier.DEFAULT);
		if(!VoltageTier.isConfigured(voltage)) return;

		addVoltageLine(block, stack, list, voltage);
	}

	private void addBatteryVoltageLine(ItemStack stack, List list) {
		long voltage = BatteryVoltageRegistry.getVoltage(stack, VoltageTier.DEFAULT);
		if(!VoltageTier.isConfigured(voltage)) return;

		list.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("hbm.voltage.generic") + ": " + VoltageTier.format(voltage));
	}

	private Block getBlock(ItemBlock itemBlock) {
		try {
			return (Block) BLOCK_FIELD.get(itemBlock);
		} catch(Exception e) {
			return null;
		}
	}

	private void addVoltageLine(Block block, ItemStack stack, List list, long voltage) {
		if(block instanceof BlockContainer) {
			try {
				TileEntity tile = ((BlockContainer) block).createNewTileEntity(null, stack.getItemDamage());
				boolean shown = false;
				if(tile instanceof IEnergyReceiverMK2) {
					list.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("hbm.voltage.input") + ": " + VoltageTier.format(voltage));
					shown = true;
				}
				if(tile instanceof IEnergyProviderMK2) {
					list.add(EnumChatFormatting.GREEN + StatCollector.translateToLocal("hbm.voltage.output") + ": " + VoltageTier.format(voltage));
					shown = true;
				}
				if(shown) return;
			} catch(Exception ignored) {
			}
		}
		list.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("hbm.voltage.generic") + ": " + VoltageTier.format(voltage));
	}
}
