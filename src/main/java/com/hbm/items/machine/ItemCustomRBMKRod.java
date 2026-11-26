package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelModel;
import com.hbm.items.machine.customfuel.CustomFuelModel.RBMKProfile;
import com.hbm.items.machine.customfuel.CustomFuelModel.RBMKTerm;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class ItemCustomRBMKRod extends ItemRBMKRod implements ICustomFuelItem {

	public static final int AMOUNT = 384;

	protected IIcon iconBG;

	public ItemCustomRBMKRod(ItemRBMKPellet pellet) {
		super(pellet);
	}

	@Override
	public int getFuelAmount() {
		return AMOUNT;
	}

	// Fills in yield and heat for a rod that only carries a mix so far
	public static ItemStack prepare(ItemStack stack) {
		if(stack.hasTagCompound() && stack.stackTagCompound.hasKey("yield")) return stack;
		setNBTDefaults(stack);
		return stack;
	}

	public static double getDepletion(ItemStack stack) {
		return 1D - getEnrichment(stack);
	}

	public static RBMKProfile getProfile(ItemStack stack) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		return mix == null ? null : CustomFuelModel.rbmk(mix, getDepletion(stack));
	}

	@Override
	public double burn(World world, ItemStack stack, double inFlux) {
		prepare(stack);
		RBMKProfile p = getProfile(stack);
		if(p == null) return 0D;

		inFlux += p.selfRate * p.selfCapture;
		if(inFlux < 0D) inFlux = 0D;

		if(RBMKDials.getXenon(world)) {
			double xenon = getPoison(stack);
			xenon -= xenonBurnFunc(inFlux);
			inFlux *= (1D - getPoisonLevel(stack));
			xenon += xenonGenFunc(inFlux) * p.xenonMult;
			setPoison(stack, Math.max(0D, Math.min(100D, xenon)));
		}

		double flux = inFlux * reactivityModByEnrichment(getEnrichment(stack));
		double outFlux = (p.react(flux) + p.selfRate * (1D - p.selfCapture) * getEnrichment(stack)) * RBMKDials.getReactivityMod(world);

		if(RBMKDials.getDepletion(world)) {
			setYield(stack, Math.max(getYield(stack) - inFlux / p.lifetime, 0D));
		}

		double coreHeat = getCoreHeat(stack) + outFlux * p.heat;
		if(coreHeat > 1_000_000D) coreHeat = 1_000_000D;
		if(coreHeat < 20D || Double.isNaN(coreHeat)) coreHeat = 20D;
		setCoreHeat(stack, coreHeat);

		return outFlux;
	}

	@Override
	public double provideHeat(World world, ItemStack stack, double heat, double mod) {
		RBMKProfile p = getProfile(stack);
		this.meltingPoint = p != null ? p.meltingPoint : 2865D;
		return super.provideHeat(world, stack, heat, mod);
	}

	@Override
	public void onCreated(ItemStack stack, World world, EntityPlayer player) {
		prepare(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for(int[] preset : PRESETS) list.add(prepare(ICustomFuelItem.makePreset(this, preset)));
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
		return mix == null ? 0xffffff : mix.getColor();
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
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.noMix"));
			return;
		}

		ICustomFuelItem.addComposition(mix, list);
		RBMKProfile p = CustomFuelModel.rbmk(mix, getDepletion(stack));

		if(getHullHeat(stack) >= 50 || getCoreHeat(stack) >= 50) {
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.wasteCooling"));
		}

		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.customFuel.model", EnumChatFormatting.RESET, CustomFuelModel.current().title));
		if(p.selfRate > 0) list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.source"));

		list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("trait.rbmk.depletion", ((int) (getDepletion(stack) * 100000D)) / 1000D + "%"));
		list.add(EnumChatFormatting.DARK_PURPLE + I18nUtil.resolveKey("trait.rbmk.xenon", ((int) (getPoison(stack) * 1000D) / 1000D) + "%"));
		list.add(EnumChatFormatting.BLUE + I18nUtil.resolveKey("trait.rbmk.splitsWith", I18nUtil.resolveKey(nType.unlocalized)));
		list.add(EnumChatFormatting.BLUE + I18nUtil.resolveKey("trait.rbmk.splitsInto", I18nUtil.resolveKey(rType.unlocalized)));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.fluxFunc", EnumChatFormatting.WHITE + getFuncDescription(stack, p)));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.funcType", getFuncTypes(p)));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.xenonGen", EnumChatFormatting.WHITE + "x * " + round(xGen * p.xenonMult)));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.xenonBurn", EnumChatFormatting.WHITE + "x² / " + xBurn));
		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("trait.rbmk.heat", round(p.heat) + "°C"));
		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("trait.rbmk.diffusion", diffusion + "¹/²"));
		list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.skinTemp", ((int) (getHullHeat(stack) * 10D) / 10D) + "°C"));
		list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("trait.rbmk.coreTemp", ((int) (getCoreHeat(stack) * 10D) / 10D) + "°C"));
		list.add(EnumChatFormatting.DARK_RED + I18nUtil.resolveKey("trait.rbmk.melt", round(p.meltingPoint) + "°C"));
	}

	// same syntax as the vanilla rods, each isotope's weight is folded into its reactivity since every burn function scales linearly with it
	public String getFuncDescription(ItemStack stack, RBMKProfile p) {

		double captured = p.selfRate * p.selfCapture;
		double leaked = p.selfRate * (1D - p.selfCapture);
		String flux = captured > 0 ? "(x" + EnumChatFormatting.RED + " + " + round(captured) + EnumChatFormatting.WHITE + ")" : "x";

		double enrichment = getEnrichment(stack);
		double mod = p.fluxMult * (enrichment < 1 ? reactivityModByEnrichment(enrichment) : 1D);
		if(mod != 1D) flux = "(" + flux + " * " + EnumChatFormatting.YELLOW + ((int) (mod * 1000D) / 1000D) + EnumChatFormatting.WHITE + ")";

		List<String> parts = new ArrayList();
		for(RBMKTerm term : p.terms) parts.add(String.format(Locale.US, getFuncTemplate(term.func), flux, round(term.reactivity * term.weight)));
		if(leaked > 0) parts.add(EnumChatFormatting.RED + "" + round(leaked * enrichment) + EnumChatFormatting.WHITE);

		String desc = parts.isEmpty() ? "0" : String.join(" + ", parts);
		if(enrichment < 1) desc += EnumChatFormatting.GOLD + " (" + ((int) (reactivityModByEnrichment(enrichment) * 1000D) / 10D) + "%)";
		return desc;
	}

	public static String getFuncTypes(RBMKProfile p) {
		List<String> types = new ArrayList();
		List<EnumBurnFunc> seen = new ArrayList();
		for(RBMKTerm term : p.terms) {
			if(seen.contains(term.func)) continue;
			seen.add(term.func);
			types.add(term.func.format + I18nUtil.resolveKey(term.func.title));
		}
		if(p.selfRate * (1D - p.selfCapture) > 0) types.add(EnumBurnFunc.PASSIVE.format + I18nUtil.resolveKey(EnumBurnFunc.PASSIVE.title));
		return String.join(EnumChatFormatting.WHITE + ", ", types);
	}

	private static double round(double d) {
		return Math.round(d * 100D) / 100D;
	}
}
