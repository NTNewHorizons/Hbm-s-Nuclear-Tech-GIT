package com.hbm.util;

import cpw.mods.fml.common.Loader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class CompatHarvestcraft {

	public static final String MODID = "harvestcraft";

	// fruits whose juice is registered as <name>juiceItem
	public static final String[] JUICE_FRUITS = new String[] {
			"blackberry", "blueberry", "cactusfruit", "cherry", "cranberry", "grape", "kiwi", "lime", "mango", "orange", "papaya",
			"peach", "pomegranate", "starfruit", "strawberry", "plum", "pear", "apricot", "fig", "grapefruit", "persimmon" };

	public static final String[] OIL_NUTS = new String[] { "cropPeanut", "cropWalnut", "cropAlmond", "cropCashew", "cropPecan", "cropPistachio", "cropChestnut", "cropCoconut" };
	public static final String[] OIL_SEEDS = new String[] { "cropOlive", "cropSunflower", "cropSoybean", "cropMustard", "cropSesame", "cropAvocado" };
	// oily items without a crop tag
	public static final String[] OIL_SEED_ITEMS = new String[] { "grapeseedItem", "cottonseedItem", "teaseedItem" };

	// Pam tags firm tofu as raw fish, which would allow soybean -> fish oil
	public static final String KEY_FISH_RAW = "hbmFishRaw";
	private static boolean fishDictRegistered = false;

	public static String fishRawDict() {
		if(!fishDictRegistered) {
			fishDictRegistered = true;
			Item tofu = item("firmtofuItem");
			for(ItemStack stack : OreDictionary.getOres("listAllfishraw")) {
				if(stack != null && stack.getItem() != null && stack.getItem() != tofu) OreDictionary.registerOre(KEY_FISH_RAW, stack.copy());
			}
		}
		return KEY_FISH_RAW;
	}

	public static boolean isLoaded() {
		return Loader.isModLoaded(MODID);
	}

	public static Item item(String name) {
		return Compat.tryLoadItem(MODID, name);
	}

	public static ItemStack stack(String name) {
		return stack(name, 1);
	}

	public static ItemStack stack(String name, int count) {
		Item item = item(name);
		return item != null ? new ItemStack(item, count) : null;
	}
}
