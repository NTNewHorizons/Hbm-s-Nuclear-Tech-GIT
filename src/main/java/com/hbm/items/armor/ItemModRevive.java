package com.hbm.items.armor;

import java.util.List;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemModRevive extends ItemArmorMod {

	public ItemModRevive(int durability) {
		super(ArmorModHandler.extra, false, false, true, false);
		this.setMaxDamage(durability);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {

		if(this == ModItems.scrumpy) {
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("item.tooltip.item_mod_revive.1"));
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("item.tooltip.item_mod_revive.2"));
		}
		if(this == ModItems.wild_p) {
			list.add(EnumChatFormatting.DARK_GRAY + I18nUtil.resolveKey("item.tooltip.item_mod_revive.3", EnumChatFormatting.RED, EnumChatFormatting.DARK_GRAY, EnumChatFormatting.RED));
		}
		
		/*list.add(I18nUtil.resolveKey("tooltip.item_mod_revive.s_in_the_news", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("tooltip.item_mod_revive.s_s_man_literally_too_angry_to_die", EnumChatFormatting.RED, EnumChatFormatting.BOLD));
		list.add("");
		list.add(I18nUtil.resolveKey("tooltip.item_mod_revive.s_i_ain_t_got_time_to_die_says_local", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("tooltip.item_mod_revive.sman_after_ripping_the_physical_manifestation", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("tooltip.item_mod_revive.sof_disaster_itself_in_half", EnumChatFormatting.ITALIC));*/
		
		list.add("");
		list.add(EnumChatFormatting.GOLD + "" + I18nUtil.resolveKey("item.tooltip.item_mod_revive.4", (stack.getMaxDamage() - stack.getItemDamage())));
		list.add("");
		super.addInformation(stack, player, list, bool);
	}

	@Override
	public void addDesc(List list, ItemStack stack, ItemStack armor) {

		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("item.tooltip.item_mod_revive.5", stack.getDisplayName(), (stack.getMaxDamage() - stack.getItemDamage())));
	}
}
