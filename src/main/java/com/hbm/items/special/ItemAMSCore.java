package com.hbm.items.special;

import java.util.List;

import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemAMSCore extends Item {

	long powerBase;
	int heatBase;
	int fuelBase;
	
	public ItemAMSCore(long powerBase, int heatBase, int fuelBase) {
		this.powerBase = powerBase;
		this.heatBase = heatBase;
		this.fuelBase = fuelBase;
	}

	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean bool) {

		if (this == ModItems.ams_core_sing) {
			list.add(I18nUtil.resolveKey("desc.item.ams_core.1"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.2"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.3"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.4"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.5"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.6"));
		}

		if (this == ModItems.ams_core_wormhole) {
			list.add(I18nUtil.resolveKey("desc.item.ams_core.7"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.8"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.9"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.10"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.11"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.12"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.13"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.14"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.15"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.16"));
		}

		if (this == ModItems.ams_core_eyeofharmony) {
			list.add(I18nUtil.resolveKey("desc.item.ams_core.17"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.18"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.19"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.20"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.21"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.22"));
			list.add(I18nUtil.resolveKey("desc.item.ams_core.23"));
		}

		if (this == ModItems.ams_core_thingy) {
			if(MainRegistry.polaroidID == 11) {
				list.add(I18nUtil.resolveKey("desc.item.ams_core.24"));
			} else {
				list.add("...");
				list.add("...");
				list.add(I18nUtil.resolveKey("desc.item.ams_core.25"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.26"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.27"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.28"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.29"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.30"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.31"));
				list.add(I18nUtil.resolveKey("desc.item.ams_core.32"));
			}
		}
	}

    @Override
	public EnumRarity getRarity(ItemStack p_77613_1_) {

    	if(this == ModItems.ams_core_thingy)
    	{
    		return EnumRarity.epic;
    	}
    	
    	return EnumRarity.uncommon;
    }

    @Override
	@SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack p_77636_1_)
    {
    	if(this == ModItems.ams_core_thingy && MainRegistry.polaroidID == 11)
    	{
    		return true;
    	}
    	
    	return false;
    }
    
    public static long getPowerBase(ItemStack stack) {
		if(stack == null || !(stack.getItem() instanceof ItemAMSCore))
			return 0;
		return ((ItemAMSCore)stack.getItem()).powerBase;
    }
    
    public static int getHeatBase(ItemStack stack) {
		if(stack == null || !(stack.getItem() instanceof ItemAMSCore))
			return 0;
		return ((ItemAMSCore)stack.getItem()).heatBase;
    }
    
    public static int getFuelBase(ItemStack stack) {
		if(stack == null || !(stack.getItem() instanceof ItemAMSCore))
			return 0;
		return ((ItemAMSCore)stack.getItem()).fuelBase;
    }
}
