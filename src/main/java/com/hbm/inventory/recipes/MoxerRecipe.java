package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class MoxerRecipe extends GenericRecipe {

	public int amount;

	public MoxerRecipe(String name) { super(name); }

	public List<String> print() {
		List<String> list = new ArrayList();
		list.add(EnumChatFormatting.YELLOW + this.getLocalizedName());

		duration(list);
		power(list);
		list.add(EnumChatFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("gui.recipe.moxerAmount") + ": " + 2 * amount + " mB");
		input(list);
		output(list);

		return list;
	}

	public MoxerRecipe setAmount(int amount) { this.amount = amount; return this; }
}
