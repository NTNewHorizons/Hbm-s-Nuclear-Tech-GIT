package com.hbm.items.tool;

import baubles.api.BaubleType;
import baubles.api.IBauble;

import cpw.mods.fml.common.Optional;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class ItemKey extends ItemKeyPin implements IBauble {

	@Override
	@Optional.Method(modid = "Baubles")
	public BaubleType getBaubleType(ItemStack stack) {
		return BaubleType.BELT;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onWornTick(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onEquipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onUnequipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canEquip(ItemStack stack, EntityLivingBase entity) {
		return true;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canUnequip(ItemStack stack, EntityLivingBase entity) {
		return true;
	}

}
