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

	// picked by dominant fissile isotope and fissile share, rbmkStage is the pellet depletion overlay 0 to 4
	public static ItemStack closestVanilla(Reactor reactor, CustomFuelMix mix, int rbmkStage) {
		EnumCustomFuel dom = mix.dominantFissile();
		double e = mix.fissileShare();
		boolean thoriumBase = mix.share(EnumCustomFuel.TH232) > mix.share(EnumCustomFuel.U238);
		boolean uraniumBase = mix.share(EnumCustomFuel.U238) > 0.5D;

		switch(reactor) {
		case RBMK: {
			Item pellet;
			switch(dom) {
			case U233: pellet = thoriumBase && e < 0.25D ? ModItems.rbmk_pellet_thmeu : ModItems.rbmk_pellet_heu233; break;
			case PU239: pellet = uraniumBase && e < 0.15D ? ModItems.rbmk_pellet_mox : e < 0.15D ? ModItems.rbmk_pellet_lep : e < 0.4D ? ModItems.rbmk_pellet_mep : ModItems.rbmk_pellet_hep239; break;
			case NP237: pellet = e < 0.4D ? ModItems.rbmk_pellet_men : ModItems.rbmk_pellet_hen; break;
			case SA326: case SA327: pellet = e < 0.15D ? ModItems.rbmk_pellet_les : e < 0.4D ? ModItems.rbmk_pellet_mes : ModItems.rbmk_pellet_hes; break;
			case PU241: pellet = ModItems.rbmk_pellet_hep241; break;
			case AM242: pellet = e < 0.15D ? ModItems.rbmk_pellet_lea : e < 0.4D ? ModItems.rbmk_pellet_mea : ModItems.rbmk_pellet_hea242; break;
			case CM245: case CM247: pellet = e < 0.15D ? ModItems.rbmk_pellet_lecm : e < 0.4D ? ModItems.rbmk_pellet_mecm : ModItems.rbmk_pellet_hecm; break;
			case BK247: pellet = ModItems.rbmk_pellet_bk247; break;
			case CF251: case ES253: pellet = e < 0.15D ? ModItems.rbmk_pellet_lecf : e < 0.4D ? ModItems.rbmk_pellet_mecf : ModItems.rbmk_pellet_hecf; break;
			case AUS: pellet = e < 0.4D ? ModItems.rbmk_pellet_leaus : ModItems.rbmk_pellet_heaus; break;
			default: pellet = e < 0.03D ? ModItems.rbmk_pellet_ueu : e < 0.25D ? ModItems.rbmk_pellet_meu : ModItems.rbmk_pellet_heu235; break;
			}
			return new ItemStack(pellet, 1, Math.max(0, Math.min(4, rbmkStage)));
		}
		case PWR: {
			EnumPWRFuel type;
			switch(dom) {
			case U233: type = EnumPWRFuel.HEU233; break;
			case PU239: type = uraniumBase && e < 0.25D ? EnumPWRFuel.MOX : e < 0.25D ? EnumPWRFuel.MEP : EnumPWRFuel.HEP239; break;
			case NP237: type = e < 0.25D ? EnumPWRFuel.MEN : EnumPWRFuel.HEN237; break;
			case SA326: type = EnumPWRFuel.HES326; break;
			case SA327: type = EnumPWRFuel.HES327; break;
			case PU241: type = EnumPWRFuel.HEP241; break;
			case AM242: case CM245: case CM247: case BK247: case CF251: case ES253: type = e < 0.25D ? EnumPWRFuel.MEA : EnumPWRFuel.HEA242; break;
			case AUS: type = EnumPWRFuel.HES326; break;
			default: type = e < 0.25D ? EnumPWRFuel.MEU : EnumPWRFuel.HEU235; break;
			}
			return new ItemStack(ModItems.pwr_fuel_depleted, 1, type.ordinal());
		}
		case ZIRNOX: {
			Item rod;
			switch(dom) {
			case U233: rod = thoriumBase ? ModItems.rod_zirnox_thorium_fuel_depleted : ModItems.rod_zirnox_u233_fuel_depleted; break;
			case PU239: case NP237: rod = uraniumBase && e < 0.25D ? ModItems.rod_zirnox_mox_fuel_depleted : ModItems.rod_zirnox_plutonium_fuel_depleted; break;
			case SA326: case SA327: case AUS: rod = ModItems.rod_zirnox_les_fuel_depleted; break;
			case PU241: case AM242: case CM245: case CM247: case BK247: case CF251: case ES253: rod = ModItems.rod_zirnox_plutonium_fuel_depleted; break;
			default: rod = e < 0.03D ? ModItems.rod_zirnox_natural_uranium_fuel_depleted : e < 0.25D ? ModItems.rod_zirnox_uranium_fuel_depleted : ModItems.rod_zirnox_u235_fuel_depleted; break;
			}
			return new ItemStack(rod);
		}
		case WATZ: {
			EnumWatzType type;
			switch(dom) {
			case PU239: type = EnumWatzType.MEP; break;
			case NP237: type = EnumWatzType.HEN; break;
			case PU241: type = EnumWatzType.PU241; break;
			case AM242: type = e < 0.5D ? EnumWatzType.AMRG : EnumWatzType.AMF; break;
			case CM245: case CM247: type = e < 0.5D ? EnumWatzType.CMRG : EnumWatzType.CMF; break;
			case BK247: type = EnumWatzType.BK247; break;
			case CF251: type = EnumWatzType.CF251; break;
			case ES253: type = EnumWatzType.ES253; break;
			case AUS: type = EnumWatzType.LES; break;
			case SA326: case SA327: type = e < 0.15D ? EnumWatzType.LES : e < 0.4D ? EnumWatzType.MES : e < 0.8D ? EnumWatzType.HES : EnumWatzType.SCHRABIDIUM; break;
			default: type = EnumWatzType.MEU; break;
			}
			return new ItemStack(ModItems.watz_pellet_depleted, 1, type.ordinal());
		}
		}
		return null;
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
