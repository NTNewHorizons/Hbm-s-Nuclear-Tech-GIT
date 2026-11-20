package com.hbm.inventory;

import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.InventoryUtil;
import com.hbm.util.fauxpointtwelve.DirPos;

import net.minecraft.block.BlockChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * {@link com.hbm.util.InventoryUtil} doesn't check side restrictions and im not
 * touching it
 */
public class MachineExport {
	public static void export(TileEntityMachineBase machine) {
		World world = machine.getWorldObj();
		if (world == null || world.isRemote)
			return;

		for (PortDef port : machine.getItemOutputPorts()) {
			for (DirPos connection : port.portConnections) {
				exportTo(machine, connection);
			}
		}
	}

	private static void exportTo(TileEntityMachineBase machine, DirPos connection) {
		World world = machine.getWorldObj();
		ForgeDirection direction = connection.getDir();
		int x = connection.getX();
		int y = connection.getY();
		int z = connection.getZ();
		if (!world.blockExists(x, y, z))
			return;

		TileEntity sourceTile = world.getTileEntity(x - direction.offsetX, y - direction.offsetY,
				z - direction.offsetZ);
		if (sourceTile != machine && (!(sourceTile instanceof TileEntityProxyBase)
				|| ((TileEntityProxyBase) sourceTile).getTE() != machine))
			return;
		if (!(sourceTile instanceof ISidedInventory))
			return;

		TileEntity targetTile = world.getTileEntity(x, y, z);
		if (targetTile == machine
				|| targetTile instanceof TileEntityProxyBase && ((TileEntityProxyBase) targetTile).getTE() == machine)
			return;
		if (!(targetTile instanceof IInventory))
			return;
		IInventory target = (IInventory) targetTile;
		if (world.getBlock(x, y, z) instanceof BlockChest) {
			target = ((BlockChest) world.getBlock(x, y, z)).func_149951_m(world, x, y, z);
			if (target == null)
				return;
		}

		ISidedInventory source = (ISidedInventory) sourceTile;
		int side = direction.ordinal();
		for (int slot : source.getAccessibleSlotsFromSide(side)) {
			ItemStack stack = source.getStackInSlot(slot);
			if (stack == null || stack.stackSize <= 0 || !source.canExtractItem(slot, stack, side))
				continue;
			int transferred = insert(target, stack, direction.getOpposite().ordinal());
			if (transferred > 0) {
				source.decrStackSize(slot, transferred);
				machine.markDirty();
				target.markDirty();
			}
		}
	}

	static int insert(IInventory target, ItemStack stack, int side) {
		ISidedInventory sided = target instanceof ISidedInventory ? (ISidedInventory) target : null;
		int[] slots;
		if (sided != null) {
			slots = sided.getAccessibleSlotsFromSide(side);
		} else {
			slots = new int[target.getSizeInventory()];
			for (int i = 0; i < slots.length; i++)
				slots[i] = i;
		}

		int remaining = stack.stackSize;
		for (int slot : slots) {
			if (!target.isItemValidForSlot(slot, stack) || sided != null && !sided.canInsertItem(slot, stack, side))
				continue;
			ItemStack existing = target.getStackInSlot(slot);
			if (existing != null && !InventoryUtil.doesStackDataMatch(existing, stack))
				continue;
			int limit = Math.min(target.getInventoryStackLimit(), stack.getMaxStackSize());
			int amount = Math.min(remaining, limit - (existing == null ? 0 : existing.stackSize));
			if (amount <= 0)
				continue;
			ItemStack inserted = stack.copy();
			inserted.stackSize = amount + (existing == null ? 0 : existing.stackSize);
			target.setInventorySlotContents(slot, inserted);
			remaining -= amount;
			if (remaining == 0)
				break;
		}
		return stack.stackSize - remaining;
	}
}
