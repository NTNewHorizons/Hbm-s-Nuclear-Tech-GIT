package com.hbm.items.tool;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemWireSpool extends Item {

	public int wireColor;

	public ItemWireSpool(int color) {
		this.wireColor = color;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		switch(this.wireColor) {
		case 0: list.add("Copper wire - LV rated"); break;
		case 1: list.add("Gold wire - MV rated"); break;
		case 2: list.add("Steel wire - HV rated"); break;
		case 3: list.add("Superconductor wire - SC rated"); break;
		}
	}
}
