package com.hbm.items.machine;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.util.i18n.I18nUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

import java.util.List;

public class ItemCustomRBMKRod extends ItemRBMKRod {

	protected IIcon iconBG;

	public ItemCustomRBMKRod(ItemRBMKPellet pellet) {
		super(pellet);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		return pass == 1 ? super.getIconFromDamageForRenderPass(meta, pass) : this.iconBG;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if (stack != null && stack.hasTagCompound() && pass == 0) {
			int[] inp = stack.stackTagCompound.getIntArray("inp");

			int size = inp.length / 2;
			if (size > 0) {
				int r = 0;
				int g = 0;
				int b = 0;
				for (int i = 0; i < size; i++) {
					NTMMaterial mat = Mats.matById.get(inp[i * 2]);
					if (mat == null) continue;
					double perc = (double) inp[i * 2 + 1] / Math.max(stack.stackTagCompound.getInteger("amount"), 1);
					r += (mat.moltenColor & 0xff0000) >> 16;
					g += (mat.moltenColor & 0x00ff00) >> 8;
					b += mat.moltenColor & 0x0000ff;
				}
				r /= size;
				g /= size;
				b /= size;
				return r << 16 | g << 8 | b;
			}
		}
		return 0xffffff;
	}

	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {

		list.add(EnumChatFormatting.ITALIC + this.fullName);

		if(ItemRBMKRod.getHullHeat(stack) >= 50 || ItemRBMKRod.getCoreHeat(stack) >= 50) {
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.wasteCooling"));
		}

		if(selfRate > 0 || this.function == EnumBurnFunc.SIGMOID) {
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.source"));
		}

		list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("trait.rbmk.depletion", ((int)(((yield - getYield(stack)) / yield) * 100000D)) / 1000D + "%"));
		list.add(EnumChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("trait.rbmk.xenon", ((int)(getPoison(stack) * 1000D) / 1000D) + "%"));
		list.add(EnumChatFormatting.BLUE + I18nUtil.resolveKey("trait.rbmk.splitsWith", I18nUtil.resolveKey(nType.unlocalized)));
		list.add(EnumChatFormatting.BLUE + I18nUtil.resolveKey("trait.rbmk.splitsInto", I18nUtil.resolveKey(rType.unlocalized)));
		//list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.fluxFunc", EnumChatFormatting.WHITE + getFuncDescription(stack)));
		if (stack != null && stack.hasTagCompound()) {
			String funcName = stack.stackTagCompound.getString("funcName");
			String funcStep = "";
			for (int i = 0; i < funcName.length(); i++) {
				if (funcName.charAt(i) == '|') {
					list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.fluxFunc", EnumChatFormatting.WHITE + funcStep));
					funcStep = "";
				} else {
					funcStep += funcName.charAt(i);
				}
			}
		} else {
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.fluxFunc", EnumChatFormatting.WHITE + "ERROR"));
		}
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.funcType", this.function.title));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.xenonGen", EnumChatFormatting.WHITE + "x * " + xGen));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.xenonBurn", EnumChatFormatting.WHITE + "x² / " + xBurn));
		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("trait.rbmk.heat", heat + "°C"));
		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("trait.rbmk.diffusion", diffusion + "¹/²"));
		list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.skinTemp", ((int)(getHullHeat(stack) * 10D) / 10D) + "°C"));
		list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.coreTemp", ((int)(getCoreHeat(stack) * 10D) / 10D) + "°C"));
		list.add(EnumChatFormatting.DARK_RED + I18nUtil.resolveKey("trait.rbmk.melt", meltingPoint + "°C"));

		super.addInformation(stack, player, list, bool);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		this.iconBG = reg.registerIcon(this.iconString + "_bg");
	}
}
