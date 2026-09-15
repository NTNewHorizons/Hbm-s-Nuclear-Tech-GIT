package com.hbm.items.machine;

import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.items.ItemCustomLore;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import api.ntm1of90.compat.fluid.registry.FluidMappingRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * Empty fluid container (canister/tank/barrel/gas tank). Can be filled from
 * Forge IFluidContainerItem consumers (AE2 terminal refill) via NTM's container registry.
 */
public class ItemFluidContainerEmpty extends ItemCustomLore implements IFluidContainerItem {

	@Override
	public FluidStack getFluid(ItemStack stack) {
		return null;
	}

	@Override
	public int getCapacity(ItemStack stack) {
		return 0;
	}

	@Override
	public int fill(ItemStack stack, FluidStack resource, boolean doFill) {
		if(stack == null || resource == null || resource.amount <= 0) return 0;
		FluidType type = FluidMappingRegistry.getHbmFluidType(resource.getFluid());
		if(type == Fluids.NONE) return 0;

		ItemStack full = FluidContainerRegistry.getFullContainer(stack, type);
		if(full == null) return 0;
		int content = FluidContainerRegistry.getFluidContent(full, type);
		if(content <= 0 || resource.amount < content) return 0;

		if(doFill) {
			stack.func_150996_a(full.getItem());
			stack.setItemDamage(full.getItemDamage());
			stack.stackTagCompound = full.stackTagCompound;
		}
		return content;
	}

	@Override
	public FluidStack drain(ItemStack stack, int maxDrain, boolean doDrain) {
		return null;
	}
}
