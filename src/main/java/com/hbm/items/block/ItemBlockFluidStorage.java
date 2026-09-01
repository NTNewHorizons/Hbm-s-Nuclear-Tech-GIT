package com.hbm.items.block;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.IPersistentNBT;

import api.ntm1of90.compat.fluid.registry.FluidMappingRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * Block item for machine fluid barrels/tanks. The block item keeps fluid content
 * in its persistent NBT when broken; this exposes that content to Forge's
 * IFluidContainerItem so AE2 terminals can drain/refill the block item directly.
 */
public class ItemBlockFluidStorage extends ItemBlockBase implements IFluidContainerItem {

	private final int defaultCapacity;

	// GameRegistry reflection resolves ctor args boxed: <init>(Block, Integer)
	public ItemBlockFluidStorage(Block block, Integer defaultCapacity) {
		super(block);
		this.defaultCapacity = defaultCapacity;
	}

	private NBTTagCompound getTankData(ItemStack stack) {
		if(stack == null || !stack.hasTagCompound()) return null;
		return stack.getTagCompound().getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY);
	}

	private FluidType getType(ItemStack stack) {
		NBTTagCompound data = getTankData(stack);
		if(data == null) return Fluids.NONE;
		return Fluids.fromID(data.getInteger("tank_type"));
	}

	private int getFill(ItemStack stack) {
		NBTTagCompound data = getTankData(stack);
		return data == null ? 0 : data.getInteger("tank");
	}

	private int getTankCapacity(ItemStack stack) {
		NBTTagCompound data = getTankData(stack);
		if(data != null && data.getInteger("tank_max") > 0) return data.getInteger("tank_max");
		return defaultCapacity;
	}

	private void setTankContents(ItemStack stack, FluidType type, int fill) {
		if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound data = stack.getTagCompound().getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY);
		data.setInteger("tank", fill);
		data.setInteger("tank_type", type.getID());
		data.setInteger("tank_max", getTankCapacity(stack));
		stack.getTagCompound().setTag(IPersistentNBT.NBT_PERSISTENT_KEY, data);
	}

	@Override
	public FluidStack getFluid(ItemStack stack) {
		FluidType type = getType(stack);
		int fill = getFill(stack);
		if(type == Fluids.NONE || fill <= 0) return null;
		net.minecraftforge.fluids.Fluid fluid = FluidMappingRegistry.getForgeFluid(type);
		return fluid == null ? null : new FluidStack(fluid, fill);
	}

	@Override
	public int getCapacity(ItemStack stack) {
		return getCapacity(stack);
	}

	@Override
	public int fill(ItemStack stack, FluidStack resource, boolean doFill) {
		if(stack == null || resource == null || resource.amount <= 0) return 0;
		FluidType type = FluidMappingRegistry.getHbmFluidType(resource.getFluid());
		if(type == Fluids.NONE || type.hasNoID()) return 0;

		FluidType current = getType(stack);
		int currentFill = getFill(stack);
		int room = getTankCapacity(stack) - currentFill;
		if(room <= 0 || (currentFill > 0 && current != type)) return 0;

		int amount = Math.min(room, resource.amount);
		if(doFill) {
			setTankContents(stack, type, currentFill + amount);
		}
		return amount;
	}

	@Override
	public FluidStack drain(ItemStack stack, int maxDrain, boolean doDrain) {
		FluidStack content = getFluid(stack);
		if(content == null || maxDrain <= 0) return null;
		int amount = Math.min(content.amount, maxDrain);
		if(doDrain) {
			int remaining = content.amount - amount;
			setTankContents(stack, remaining > 0 ? getType(stack) : Fluids.NONE, remaining);
		}
		return new FluidStack(content.getFluid(), amount);
	}
}
