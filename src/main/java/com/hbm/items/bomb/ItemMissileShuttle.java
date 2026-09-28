package com.hbm.items.bomb;

import com.hbm.util.i18n.I18nUtil;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemMissileShuttle extends Item {

	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean bool) {
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.1"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.2"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.3"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.4"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.5"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.6"));
		list.add(I18nUtil.resolveKey("item.tooltip.item_missile_shuttle.7"));
	}
}