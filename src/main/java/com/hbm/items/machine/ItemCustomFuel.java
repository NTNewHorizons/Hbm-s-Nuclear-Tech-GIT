package com.hbm.items.machine;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelModel;
import com.hbm.items.machine.customfuel.CustomFuelModel.FuncTerm;
import com.hbm.items.machine.customfuel.CustomFuelModel.PWRProfile;
import com.hbm.items.machine.customfuel.CustomFuelModel.WatzProfile;
import com.hbm.items.machine.customfuel.CustomFuelModel.ZirnoxProfile;
import com.hbm.items.machine.customfuel.CustomFuelSpent;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

public class ItemCustomFuel extends Item implements ICustomFuelItem {

	public static enum Form {
		INGOT(72, false),
		BILLET(48, false),
		PWR(96, false),
		PWR_HOT(96, true),
		PWR_DEPLETED(96, true),
		ZIRNOX(96, false),
		ZIRNOX_DEPLETED(96, true),
		WATZ(288, false),
		WATZ_DEPLETED(288, true);

		public final int amount;
		public final boolean spent;

		private Form(int amount, boolean spent) {
			this.amount = amount;
			this.spent = spent;
		}
	}

	public static final double WATZ_YIELD = 1_000_000_000D;

	public final Form form;
	protected IIcon iconBG;

	public ItemCustomFuel(Form form) {
		this.form = form;
		this.setMaxStackSize(form == Form.ZIRNOX || form == Form.ZIRNOX_DEPLETED ? 1 : 64);
	}

	@Override
	public int getFuelAmount() {
		return form.amount;
	}

	public static ItemStack setup(ItemStack stack, List<MaterialStack> mats, int amount) {
		CustomFuelMix mix = new CustomFuelMix(mats);
		mix.fresh = amount;
		return mix.writeTo(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		if(form.spent) return;
		for(int[] preset : ICustomFuelItem.PRESETS) list.add(ICustomFuelItem.makePreset(this, preset));
	}


	public static int getLife(ItemStack stack) {
		return stack.hasTagCompound() ? stack.stackTagCompound.getInteger("life") : 0;
	}

	public static void setLife(ItemStack stack, int life) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setInteger("life", life);
	}


	public static double getWatzYield(ItemStack stack) {
		if(!stack.hasTagCompound() || !stack.stackTagCompound.hasKey("yield")) return WATZ_YIELD;
		return stack.stackTagCompound.getDouble("yield");
	}

	public static void setWatzYield(ItemStack stack, double yield) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setDouble("yield", yield);
	}

	public static double getWatzDepletion(ItemStack stack) {
		return 1D - getWatzYield(stack) / WATZ_YIELD;
	}

	public static double getZirnoxDepletion(ItemStack stack) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) return 0D;
		ZirnoxProfile p = CustomFuelModel.zirnox(mix, 0D);
		return Math.min((double) getLife(stack) / p.maxLife, 1D);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getDurabilityForDisplay(stack) > 0D;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		if(form == Form.ZIRNOX) return getZirnoxDepletion(stack);
		if(form == Form.WATZ) return getWatzDepletion(stack);
		return 0D;
	}


	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		return pass == 1 ? super.getIconFromDamageForRenderPass(meta, pass) : this.iconBG;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if(pass != 0) return 0xffffff;
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		// the sample cycles through colours since its mix is whatever the player sets
		if(mix == null) return ICustomFuelItem.isSample(stack) ? Color.HSBtoRGB((System.currentTimeMillis() % 6000L) / 6000F, 0.5F, 0.9F) & 0xffffff : 0xffffff;
		int color = mix.getColor();
		return form.spent ? ICustomFuelItem.darken(color) : color;
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


	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) {
			if(ICustomFuelItem.isSample(stack)) ICustomFuelItem.addSampleInfo(stack, list);
			else list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.noMix"));
			return;
		}

		ICustomFuelItem.addComposition(mix, list);

		String color = EnumChatFormatting.GOLD + "";
		String reset = EnumChatFormatting.RESET + "";
		String model = CustomFuelModel.current().title;

		// same lines and keys as the vanilla fuels of each reactor
		switch(form) {
		case PWR: {
			PWRProfile p = CustomFuelModel.pwr(mix, 0D);
			list.add(color + I18nUtil.resolveKey("desc.item.customFuel.model", reset, model));
			if(p.selfFlux > 0) {
				list.add(color + I18nUtil.resolveKey("item.watz_pellet.fission_rate", reset, round(p.selfFlux)));
				list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("item.watz_pellet.self_igniting"));
			}
			list.add(color + I18nUtil.resolveKey("item.pwr_fuel.heat", reset, round(p.heat)));
			list.add(color + I18nUtil.resolveKey("item.pwr_fuel.reaction", reset, describe(p.terms, p.fluxMult)));
			list.add(color + I18nUtil.resolveKey("item.pwr_fuel.fuel_type", reset, dangers(p.terms)));
			if(p.yield != PWRProfile.BASE_YIELD) list.add(color + I18nUtil.resolveKey("desc.item.customFuel.yield", reset, BobMathUtil.getShortNumber((long) p.yield)));
			break;
		}
		case ZIRNOX: {
			ZirnoxProfile p = CustomFuelModel.zirnox(mix, getZirnoxDepletion(stack));
			list.add(color + I18nUtil.resolveKey("desc.item.customFuel.model", reset, model));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.depletion", ((int) (getZirnoxDepletion(stack) * 100000D)) / 1000D + "%"));
			String[] loc = p.breeding
					? I18nUtil.resolveKeyArray("desc.item.zirnoxBreedingRod", BobMathUtil.getShortNumber(p.maxLife))
					: I18nUtil.resolveKeyArray("desc.item.zirnoxRod", p.heat, BobMathUtil.getShortNumber(p.maxLife));
			for(String line : loc) list.add(line);
			break;
		}
		case WATZ: {
			WatzProfile p = CustomFuelModel.watz(mix, getWatzDepletion(stack));
			list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("item.pile_rod_mk2.depletion", String.format(Locale.US, "%.1f", getWatzDepletion(stack) * 100D)));
			list.add(color + I18nUtil.resolveKey("desc.item.customFuel.model", reset, model));
			if(p.passive > 0) {
				list.add(color + I18nUtil.resolveKey("item.watz_pellet.fission_rate", reset, round(p.passive)));
				list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("item.watz_pellet.self_igniting"));
			}
			if(p.heatEmission > 0) list.add(color + I18nUtil.resolveKey("item.pwr_fuel.heat", reset, round(p.heatEmission)));
			if(!p.burn.isEmpty()) {
				list.add(color + I18nUtil.resolveKey("item.pwr_fuel.reaction", reset, describe(p.burn, 1D)));
				list.add(color + I18nUtil.resolveKey("item.pwr_fuel.fuel_type", reset, dangers(p.burn)));
			}
			if(!p.heatDiv.isEmpty()) list.add(color + I18nUtil.resolveKey("item.watz_pellet.thermal_multiplier", reset, describe(p.heatDiv, 1D)));
			if(!p.absorb.isEmpty()) list.add(color + I18nUtil.resolveKey("item.watz_pellet.flux_capture", reset, describe(p.absorb, 1D)));
			break;
		}
		default: break;
		}

		if(form == Form.PWR_HOT) list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.hot"));
		if(form.spent && form != Form.PWR_HOT) ICustomFuelItem.addSpentInfo(stack, CustomFuelSpent.reactorOf(this), mix, list);
	}

	// a blend of functions in the vanilla label syntax, each prefixed with its share
	private static String describe(List<FuncTerm> terms, double fluxMult) {
		List<String> parts = new ArrayList();
		for(FuncTerm term : terms) parts.add(terms.size() == 1 && term.weight == 1D ? term.func.getLabelForFuel() : round(term.weight) + " * " + term.func.getLabelForFuel());
		String desc = String.join(" + ", parts);
		if(fluxMult != 1D) desc += EnumChatFormatting.GOLD + " (" + ((int) (fluxMult * 1000D) / 10D) + "%)";
		return desc;
	}

	private static String dangers(List<FuncTerm> terms) {
		List<String> types = new ArrayList();
		for(FuncTerm term : terms) {
			String danger = term.func.getDangerFromFuel();
			if(!types.contains(danger)) types.add(danger);
		}
		return String.join(EnumChatFormatting.RESET + ", ", types);
	}

	private static double round(double d) {
		return Math.round(d * 100D) / 100D;
	}
}
