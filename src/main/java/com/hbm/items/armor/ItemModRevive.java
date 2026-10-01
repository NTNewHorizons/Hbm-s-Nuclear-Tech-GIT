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
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("item.mod_revive.desc1"));
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("item.mod_revive.desc2"));
		}
		if(this == ModItems.wild_p) {
			list.add(EnumChatFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.item.mod_revive.explosive_reactive_plot_armor", EnumChatFormatting.RED, EnumChatFormatting.DARK_GRAY, EnumChatFormatting.RED));
		}

		/*list.add(I18nUtil.resolveKey("desc.item.mod_revive.in_the_news", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("desc.item.man_literally_too_angry_die", EnumChatFormatting.RED, EnumChatFormatting.BOLD));
		list.add("");
		list.add(I18nUtil.resolveKey("desc.item.mod_revive.i_ain_t_got_time", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("desc.item.mod_revive.man_after_ripping_physical", EnumChatFormatting.ITALIC));
		list.add(I18nUtil.resolveKey("desc.item.disaster_itself_half", EnumChatFormatting.ITALIC));*/

		list.add("");
		list.add(EnumChatFormatting.GOLD + "" + I18nUtil.resolveKey("desc.item.mod_revive.revives_left", (stack.getMaxDamage() - stack.getItemDamage())));
		list.add("");
		super.addInformation(stack, player, list, bool);
	}

	@Override
	public void addDesc(List list, ItemStack stack, ItemStack armor) {

		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("item.mod_revive.desc5", stack.getDisplayName(), (stack.getMaxDamage() - stack.getItemDamage())));
	}
}
