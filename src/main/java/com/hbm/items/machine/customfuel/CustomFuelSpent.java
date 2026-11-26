package com.hbm.items.machine.customfuel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hbm.config.MachineConfig;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemWatzPellet.EnumWatzType;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class CustomFuelSpent {

	public static enum Reactor { RBMK, PWR, ZIRNOX, WATZ }

	public static ItemStack spent(Reactor reactor, CustomFuelMix fresh) {
		CustomFuelMix burned = fresh.burnUp(1D);
		if(MachineConfig.customFuelDirectVanilla()) {
			ItemStack vanilla = closestVanilla(reactor, burned, 4);
			if(vanilla == null) return waste(fresh.fresh);
			// PWR fuel leaves the core hot and cools down in the fuel pool like vanilla fuel
			if(reactor == Reactor.PWR) vanilla = new ItemStack(ModItems.pwr_fuel_hot, 1, vanilla.getItemDamage());
			return vanilla;
		}

		Item item = null;
		switch(reactor) {
		case PWR: item = ModItems.custom_fuel_pwr_hot; break;
		case ZIRNOX: item = ModItems.custom_fuel_zirnox_depleted; break;
		case WATZ: item = ModItems.custom_fuel_watz_depleted; break;
		case RBMK: item = ModItems.custom_fuel_rbmk_pellet; break;
		}
		return burned.writeTo(new ItemStack(item));
	}

	public static boolean isSpent(ItemStack stack) {
		if(stack == null) return false;
		Item item = stack.getItem();
		return item == ModItems.custom_fuel_pwr_hot || item == ModItems.custom_fuel_pwr_depleted || item == ModItems.custom_fuel_zirnox_depleted
				|| item == ModItems.custom_fuel_watz_depleted || item == ModItems.custom_fuel_rbmk_pellet;
	}

	public static Reactor reactorOf(Item item) {
		if(item == ModItems.custom_fuel_pwr_hot || item == ModItems.custom_fuel_pwr_depleted) return Reactor.PWR;
		if(item == ModItems.custom_fuel_zirnox_depleted) return Reactor.ZIRNOX;
		if(item == ModItems.custom_fuel_watz_depleted) return Reactor.WATZ;
		if(item == ModItems.custom_fuel_rbmk_pellet) return Reactor.RBMK;
		return null;
	}

	// fresh fuel needs at least this share of the vanilla concentration, the rest is rounding
	public static final double CONCENTRATION_TOLERANCE = 0.9D;

	// fresh share of an isotope, the fission products are put back on the fissile isotopes they came from
	public static double freshShare(CustomFuelMix mix, EnumCustomFuel fuel) {
		int fissile = 0;
		for(EnumCustomFuel f : EnumCustomFuel.values()) if(f.fissile) fissile += mix.get(f);
		int fresh = Math.max(mix.fresh, mix.total());
		if(fresh <= 0) return 0D;
		if(!fuel.fissile || fissile <= 0) return (double) mix.get(fuel) / fresh;
		int products = Math.max(mix.fresh - mix.total(), 0);
		return (double) mix.get(fuel) * (fissile + products) / fissile / fresh;
	}

	// the vanilla fuel whose concentration of one of the fissile isotopes the mix reaches, null if there is none
	public static ItemStack closestVanilla(Reactor reactor, CustomFuelMix mix, int rbmkStage) {
		List<EnumCustomFuel> fissiles = new ArrayList();
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) if(fuel.fissile && mix.get(fuel) > 0) fissiles.add(fuel);
		fissiles.sort(Comparator.comparingInt(mix::get).reversed());

		boolean thoriumBase = mix.share(EnumCustomFuel.TH232) > mix.share(EnumCustomFuel.U238);
		boolean uraniumBase = mix.share(EnumCustomFuel.U238) > 0.5D;
		int stage = Math.max(0, Math.min(4, rbmkStage));

		for(EnumCustomFuel fuel : fissiles) {
			List<Object[]> options = new ArrayList();
			options(reactor, fuel, thoriumBase, uraniumBase, stage, options);
			double share = freshShare(mix, fuel);
			ItemStack best = null;
			double bestNominal = -1;
			for(Object[] option : options) {
				double nominal = (Double) option[0];
				if(share >= nominal * CONCENTRATION_TOLERANCE && nominal > bestNominal) { best = (ItemStack) option[1]; bestNominal = nominal; }
			}
			if(best != null) return best;
		}
		return null;
	}

	// nominal fresh share of the isotope in each vanilla fuel, taken from the vanilla crafting recipes
	private static void options(Reactor reactor, EnumCustomFuel fuel, boolean thoriumBase, boolean uraniumBase, int stage, List<Object[]> out) {
		final double LOW = 2D / 9D, RG = 2D / 3D, FULL = 1D;
		switch(reactor) {
		case RBMK:
			switch(fuel) {
			case U235: rbmk(out, 1D / 12D, ModItems.rbmk_pellet_ueu, stage); rbmk(out, 1D / 6D, ModItems.rbmk_pellet_meu, stage); rbmk(out, FULL, ModItems.rbmk_pellet_heu235, stage); break;
			case U233: if(thoriumBase) rbmk(out, 1D / 6D, ModItems.rbmk_pellet_thmeu, stage); rbmk(out, FULL, ModItems.rbmk_pellet_heu233, stage); break;
			case PU239: rbmk(out, LOW, ModItems.rbmk_pellet_lep, stage); if(uraniumBase) rbmk(out, 1D / 3D, ModItems.rbmk_pellet_mox, stage); rbmk(out, RG, ModItems.rbmk_pellet_mep, stage); rbmk(out, FULL, ModItems.rbmk_pellet_hep239, stage); break;
			case PU241: rbmk(out, FULL, ModItems.rbmk_pellet_hep241, stage); break;
			case NP237: rbmk(out, 1D / 3D, ModItems.rbmk_pellet_men, stage); rbmk(out, FULL, ModItems.rbmk_pellet_hen, stage); break;
			case SA326: case SA327: rbmk(out, 1D / 9D, ModItems.rbmk_pellet_les, stage); rbmk(out, 1D / 3D, ModItems.rbmk_pellet_mes, stage); rbmk(out, 5D / 9D, ModItems.rbmk_pellet_hes, stage); break;
			case AM242: rbmk(out, LOW, ModItems.rbmk_pellet_lea, stage); rbmk(out, RG, ModItems.rbmk_pellet_mea, stage); rbmk(out, FULL, ModItems.rbmk_pellet_hea242, stage); break;
			case CM245: case CM247: rbmk(out, LOW, ModItems.rbmk_pellet_lecm, stage); rbmk(out, RG, ModItems.rbmk_pellet_mecm, stage); rbmk(out, FULL, ModItems.rbmk_pellet_hecm, stage); break;
			case BK247: rbmk(out, FULL, ModItems.rbmk_pellet_bk247, stage); break;
			case CF251: case ES253: rbmk(out, LOW, ModItems.rbmk_pellet_lecf, stage); rbmk(out, RG, ModItems.rbmk_pellet_mecf, stage); rbmk(out, FULL, ModItems.rbmk_pellet_hecf, stage); break;
			case AUS: rbmk(out, 1D / 3D, ModItems.rbmk_pellet_leaus, stage); rbmk(out, FULL, ModItems.rbmk_pellet_heaus, stage); break;
			default: break;
			}
			break;
		case PWR:
			switch(fuel) {
			case U235: pwr(out, 1D / 6D, EnumPWRFuel.MEU); pwr(out, FULL, EnumPWRFuel.HEU235); break;
			case U233: pwr(out, FULL, EnumPWRFuel.HEU233); break;
			case PU239: if(uraniumBase) pwr(out, 1D / 3D, EnumPWRFuel.MOX); pwr(out, RG, EnumPWRFuel.MEP); pwr(out, FULL, EnumPWRFuel.HEP239); break;
			case PU241: pwr(out, FULL, EnumPWRFuel.HEP241); break;
			case NP237: pwr(out, 1D / 3D, EnumPWRFuel.MEN); pwr(out, FULL, EnumPWRFuel.HEN237); break;
			case SA326: case AUS: pwr(out, FULL, EnumPWRFuel.HES326); break;
			case SA327: pwr(out, FULL, EnumPWRFuel.HES327); break;
			case AM242: case CM245: case CM247: case BK247: case CF251: case ES253: pwr(out, RG, EnumPWRFuel.MEA); pwr(out, FULL, EnumPWRFuel.HEA242); break;
			default: break;
			}
			break;
		case ZIRNOX:
			switch(fuel) {
			case U235: zirnox(out, 1D / 12D, ModItems.rod_zirnox_natural_uranium_fuel_depleted); zirnox(out, 1D / 6D, ModItems.rod_zirnox_uranium_fuel_depleted); zirnox(out, FULL, ModItems.rod_zirnox_u235_fuel_depleted); break;
			case U233: if(thoriumBase) zirnox(out, 1D / 6D, ModItems.rod_zirnox_thorium_fuel_depleted); zirnox(out, FULL, ModItems.rod_zirnox_u233_fuel_depleted); break;
			case PU239: case NP237: zirnox(out, LOW, ModItems.rod_zirnox_plutonium_fuel_depleted); if(uraniumBase) zirnox(out, 1D / 3D, ModItems.rod_zirnox_mox_fuel_depleted); break;
			case PU241: case AM242: case CM245: case CM247: case BK247: case CF251: case ES253: zirnox(out, LOW, ModItems.rod_zirnox_plutonium_fuel_depleted); break;
			case SA326: case SA327: case AUS: zirnox(out, 1D / 9D, ModItems.rod_zirnox_les_fuel_depleted); break;
			default: break;
			}
			break;
		case WATZ:
			switch(fuel) {
			case U233: case U235: watz(out, 1D / 6D, EnumWatzType.MEU); break;
			case PU239: watz(out, RG, EnumWatzType.MEP); break;
			case NP237: watz(out, FULL, EnumWatzType.HEN); break;
			case PU241: watz(out, FULL, EnumWatzType.PU241); break;
			case AM242: watz(out, LOW, EnumWatzType.AMF); watz(out, RG, EnumWatzType.AMRG); break;
			case CM245: case CM247: watz(out, LOW, EnumWatzType.CMF); watz(out, RG, EnumWatzType.CMRG); break;
			case BK247: watz(out, FULL, EnumWatzType.BK247); break;
			case CF251: watz(out, FULL, EnumWatzType.CF251); break;
			case ES253: watz(out, FULL, EnumWatzType.ES253); break;
			case AUS: watz(out, 1D / 9D, EnumWatzType.LES); break;
			case SA326: case SA327: watz(out, 1D / 9D, EnumWatzType.LES); watz(out, 1D / 3D, EnumWatzType.MES); watz(out, 5D / 9D, EnumWatzType.HES); watz(out, FULL, EnumWatzType.SCHRABIDIUM); break;
			default: break;
			}
			break;
		}
	}

	private static void rbmk(List<Object[]> out, double nominal, Item pellet, int stage) { out.add(new Object[] { nominal, new ItemStack(pellet, 1, stage) }); }
	private static void pwr(List<Object[]> out, double nominal, EnumPWRFuel type) { out.add(new Object[] { nominal, new ItemStack(ModItems.pwr_fuel_depleted, 1, type.ordinal()) }); }
	private static void zirnox(List<Object[]> out, double nominal, Item rod) { out.add(new Object[] { nominal, new ItemStack(rod) }); }
	private static void watz(List<Object[]> out, double nominal, EnumWatzType type) { out.add(new Object[] { nominal, new ItemStack(ModItems.watz_pellet_depleted, 1, type.ordinal()) }); }

	// what direct vanilla mode leaves behind when the mix matches no vanilla fuel
	public static ItemStack waste(int quanta) {
		return new ItemStack(ModItems.nuclear_waste_tiny, Math.max(quanta / 8, 1));
	}

	// at most five isotopes, PUREX has six output slots and the last one takes the waste
	public static List<ItemStack> purexOutputs(CustomFuelMix mix) {
		List<EnumCustomFuel> present = new ArrayList();
		int lost = Math.max(mix.fresh - mix.total(), 0);
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if(mix.get(fuel) >= 8 && fuel.nugget() != null) present.add(fuel);
			else lost += mix.get(fuel);
		}
		present.sort(Comparator.comparingInt(mix::get).reversed());

		List<ItemStack> out = new ArrayList();
		for(int i = 0; i < present.size(); i++) {
			EnumCustomFuel fuel = present.get(i);
			if(i < 5) out.add(new ItemStack(fuel.nugget(), mix.get(fuel) / 8));
			else lost += mix.get(fuel);
		}
		if(lost >= 8) out.add(new ItemStack(ModItems.nuclear_waste_tiny, Math.min(lost / 8, 64)));
		return out;
	}
}
