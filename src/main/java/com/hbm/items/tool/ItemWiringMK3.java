package com.hbm.items.tool;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.network.TileEntityPylonBaseMK3;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ItemWiringMK3 extends Item {

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {

		if(!player.isSneaking()) {

			Block b = world.getBlock(x, y, z);
			if(b instanceof BlockDummyable) {
				int[] core = ((BlockDummyable) b).findCore(world, x, y, z);
				if(core != null) { x = core[0]; y = core[1]; z = core[2]; }
			}

			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityPylonBaseMK3) {

				if(stack.stackTagCompound == null) {
					stack.stackTagCompound = new NBTTagCompound();
					stack.stackTagCompound.setInteger("x", x);
					stack.stackTagCompound.setInteger("y", y);
					stack.stackTagCompound.setInteger("z", z);
					if(!world.isRemote) player.addChatMessage(new ChatComponentText("Wire start"));
				} else if(!world.isRemote) {
					int x1 = stack.stackTagCompound.getInteger("x");
					int y1 = stack.stackTagCompound.getInteger("y");
					int z1 = stack.stackTagCompound.getInteger("z");

					if(world.getTileEntity(x1, y1, z1) instanceof TileEntityPylonBaseMK3) {
						TileEntityPylonBaseMK3 first = (TileEntityPylonBaseMK3) world.getTileEntity(x1, y1, z1);
						TileEntityPylonBaseMK3 second = (TileEntityPylonBaseMK3) te;

						switch(TileEntityPylonBaseMK3.canConnect(first, second)) {
						case 0: {
							int spoolSlot = findWireSpool(player);
							if(spoolSlot == -1) {
								player.addChatMessage(new ChatComponentText("Wire error - No wire spool in inventory"));
							} else {
								first.addConnection(x, y, z);
								second.addConnection(x1, y1, z1);
								player.inventory.decrStackSize(spoolSlot, 1);
								player.addChatMessage(new ChatComponentText("Wire end"));
							}
							break;
						}
						case 1:
							player.addChatMessage(new ChatComponentText("Wire error - Pylons are not the same type"));
							break;
						case 2:
							player.addChatMessage(new ChatComponentText("Wire error - Cannot connect to the same pylon"));
							break;
						case 3:
							player.addChatMessage(new ChatComponentText("Wire error - Pylon is too far away"));
							break;
						}

						stack.stackTagCompound = null;
					} else {
						if(!world.isRemote) player.addChatMessage(new ChatComponentText("Wire error"));
						stack.stackTagCompound = null;
					}
				}

				player.swingItem();
				return true;
			}
		}

		return false;
	}

	private int findWireSpool(EntityPlayer player) {
		for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
			ItemStack slot = player.inventory.getStackInSlot(i);
			if(slot != null && slot.getItem() instanceof ItemWireSpool) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		if(stack.stackTagCompound != null) {
			list.add("Wire start x: " + stack.stackTagCompound.getInteger("x"));
			list.add("Wire start y: " + stack.stackTagCompound.getInteger("y"));
			list.add("Wire start z: " + stack.stackTagCompound.getInteger("z"));
		} else {
			list.add("Right-click pylons to connect");
			list.add("Requires a wire spool in inventory");
		}
	}
}
