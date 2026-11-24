package com.hbm.items.machine;

import java.util.List;

import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSpent.Reactor;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

// keeps the burned mix, meta is the vanilla depletion stage
public class ItemCustomRBMKPellet extends ItemRBMKPellet implements ICustomFuelItem {

	public ItemCustomRBMKPellet() {
		super("item.custom_fuel_rbmk_pellet.full");
	}

	@Override
	public int getFuelAmount() {
		return ItemCustomRBMKRod.AMOUNT / 8;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tabs, List list) { }

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if(pass != 0) return 0xffffff;
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		return mix == null ? 0xffffff : mix.getColor();
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) {
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.noMix"));
		} else {
			ICustomFuelItem.addComposition(mix, list);
		}
		super.addInformation(stack, player, list, bool);
		if(mix != null) ICustomFuelItem.addSpentInfo(stack, Reactor.RBMK, mix, list);
	}
}
