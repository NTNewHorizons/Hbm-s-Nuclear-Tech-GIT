package com.hbm.items.machine.customfuel;

import java.util.ArrayList;
import java.util.List;

import com.hbm.config.MachineConfig;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.util.function.Function;
import com.hbm.util.function.Function.FunctionLinear;
import com.hbm.util.function.Function.FunctionLogarithmic;
import com.hbm.util.function.Function.FunctionQuadratic;
import com.hbm.util.function.Function.FunctionSqrt;
import com.hbm.util.function.Function.FunctionSqrtFalling;

// WEIGHTED: each fissile isotope uses its native curve scaled by its share, low enrichment is weak
// DERIVED: the mix becomes an equivalent classic fuel, enrichment tier picks the curve, potency its strength
// HYBRID: native curves plus neutron economy, fertile isotopes absorb but breed, schrabidium catalyses,
// diverse mixes run cooler, thorium suppresses xenon and sludge
public class CustomFuelModel {

	public static enum Model {
		WEIGHTED("Weighted Sum"),
		DERIVED("Derived Stats"),
		HYBRID("Hybrid");

		public final String title;
		private Model(String title) { this.title = title; }
	}

	public static Model current() {
		Model[] models = Model.values();
		return models[Math.max(0, Math.min(models.length - 1, MachineConfig.customFuelModel))];
	}

	// self-inflicted RBMK flux per unit of selfInfluence, a pure Pu238Be source is 40
	public static final double RBMK_SELF_SCALE = 40D;
	// PWR flux per rod per unit of selfInfluence, a neutron source block gives 20
	public static final double PWR_SELF_SCALE = 20D;
	public static final double HYBRID_ENRICHMENT_EXP = 0.35D;

	/// NATIVE PROFILES ///

	// how a pure fissile isotope behaves in each reactor, mostly taken from the closest vanilla high-enriched fuel
	private static class Native {
		final EnumBurnFunc rbmkFunc; final double rbmkStats, rbmkHeat;
		final Function pwrFunc; final double pwrHeat;
		final double zirnoxHeat, zirnoxLife;
		final Function watzBurn, watzDiv; final double watzHeat, watzPassive, watzMud;

		Native(EnumBurnFunc rbmkFunc, double rbmkStats, double rbmkHeat, Function pwrFunc, double pwrHeat, double zirnoxHeat, double zirnoxLife,
				Function watzBurn, double watzHeat, double watzPassive, double watzMud, Function watzDiv) {
			this.rbmkFunc = rbmkFunc; this.rbmkStats = rbmkStats; this.rbmkHeat = rbmkHeat;
			this.pwrFunc = pwrFunc; this.pwrHeat = pwrHeat;
			this.zirnoxHeat = zirnoxHeat; this.zirnoxLife = zirnoxLife;
			this.watzBurn = watzBurn; this.watzHeat = watzHeat; this.watzPassive = watzPassive; this.watzMud = watzMud; this.watzDiv = watzDiv;
		}
	}

	private static final Native[] NATIVE = new Native[EnumCustomFuel.values().length];

	private static void put(EnumCustomFuel fuel, Native n) { NATIVE[fuel.ordinal()] = n; }

	static {
		//														RBMK							PWR							ZIRNOX			WATZ
		put(EnumCustomFuel.U233,	new Native(EnumBurnFunc.LINEAR, 27.5, 1.25,		new FunctionSqrt(25), 7.5,		100, 150_000,	new FunctionSqrt(90), 10, 0, 0.0005, new FunctionSqrtFalling(10)));
		put(EnumCustomFuel.U235,	new Native(EnumBurnFunc.SQUARE_ROOT, 50, 1.0,		new FunctionSqrt(22.5), 7.5,	85, 165_000,	new FunctionSqrt(75), 10, 0, 0.0005, new FunctionSqrtFalling(10)));
		put(EnumCustomFuel.PU239,	new Native(EnumBurnFunc.LINEAR, 30, 1.25,			new FunctionSqrt(22.5), 10,		65, 175_000,	new FunctionSqrt(150), 15, 0, 0.0005, new FunctionSqrtFalling(10)));
		put(EnumCustomFuel.PU241,	new Native(EnumBurnFunc.LINEAR, 40, 1.75,			new FunctionSqrt(25), 10,		90, 150_000,	new FunctionLinear(1.30), 25, 1_950, 0.0025, new FunctionSqrt(2.66 / 18).withOff(24 * 24)));
		put(EnumCustomFuel.NP237,	new Native(EnumBurnFunc.SQUARE_ROOT, 40, 1.0,		new FunctionSqrt(27.5), 7.5,	80, 170_000,	new FunctionSqrt(100), 10, 0, 0.0005, new FunctionSqrtFalling(10)));
		put(EnumCustomFuel.AM242,	new Native(EnumBurnFunc.LINEAR, 45, 2.0,			new FunctionSqrt(25), 10,		95, 150_000,	new FunctionLinear(1.33), 44, 2_333, 0.003, new FunctionSqrt(4.11 / 22.2).withOff(27 * 27)));
		put(EnumCustomFuel.CM245,	new Native(EnumBurnFunc.SLOW_LINEAR, 60, 1.3,		new FunctionSqrt(28), 11,		110, 140_000,	new FunctionLinear(1.8), 48, 2_444, 0.0045, new FunctionSqrt(5.0 / 20).withOff(26 * 24)));
		put(EnumCustomFuel.CM247,	new Native(EnumBurnFunc.SLOW_LINEAR, 70, 1.4,		new FunctionSqrt(30), 12,		120, 130_000,	new FunctionLinear(1.6), 52, 2_600, 0.005, new FunctionSqrt(5.0 / 20).withOff(26 * 24)));
		put(EnumCustomFuel.BK247,	new Native(EnumBurnFunc.LINEAR, 50, 2.0,			new FunctionSqrt(30), 12.5,		130, 130_000,	new FunctionLinear(1.5), 55, 3_000, 0.012, new FunctionSqrt(6.0 / 23.5).withOff(10 * 10)));
		put(EnumCustomFuel.CF251,	new Native(EnumBurnFunc.LINEAR, 55, 2.2,			new FunctionSqrt(32), 13,		140, 120_000,	new FunctionLinear(1.7), 60, 1_250, 0.001, new FunctionSqrt(6.65 / 23.5).withOff(10 * 10)));
		put(EnumCustomFuel.ES253,	new Native(EnumBurnFunc.LINEAR, 60, 2.5,			new FunctionSqrt(34), 14,		160, 110_000,	new FunctionLinear(1.3), 70, 3_750, 0.0001, new FunctionSqrt(7.0 / 27.7).withOff(10 * 10)));
		put(EnumCustomFuel.SA326,	new Native(EnumBurnFunc.LINEAR, 90, 1.75,			new FunctionSqrt(27.5), 12.5,	150, 150_000,	new FunctionLinear(1.5), 20, 2_000, 0.01, new FunctionSqrtFalling(10)));
		put(EnumCustomFuel.SA327,	new Native(EnumBurnFunc.LINEAR, 75, 1.5,			new FunctionSqrt(30), 12.5,		170, 140_000,	new FunctionLinear(1.25), 20, 1_750, 0.005, new FunctionSqrtFalling(15)));
		put(EnumCustomFuel.AUS,		new Native(EnumBurnFunc.LINEAR, 35, 1.5,			new FunctionSqrt(27.5), 10,		120, 200_000,	new FunctionLinear(1.2), 20, 1_000, 0.001, new FunctionSqrtFalling(15)));
	}

	private static Native nat(EnumCustomFuel fuel) { return NATIVE[fuel.ordinal()]; }

	public static EnumBurnFunc rbmkFunc(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).rbmkFunc : EnumBurnFunc.SQUARE_ROOT; }
	public static double rbmkStats(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).rbmkStats : 0D; }
	public static double rbmkHeat(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).rbmkHeat : 1D; }
	public static Function pwrFunc(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).pwrFunc : null; }
	public static double pwrHeat(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).pwrHeat : 7.5D; }
	public static double zirnoxHeat(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).zirnoxHeat : 0D; }
	public static double zirnoxLife(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).zirnoxLife : 250_000D; }
	public static Function watzBurn(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).watzBurn : null; }
	public static Function watzHeatDiv(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).watzDiv : new FunctionSqrtFalling(10D); }
	public static double watzHeat(EnumCustomFuel fuel) { return nat(fuel) != null ? nat(fuel).watzHeat : 10D; }

	public static Function watzAbsorb(EnumCustomFuel fuel) {
		if(fuel == EnumCustomFuel.U238) return new FunctionQuadratic(1D, 1D).withDiv(100);
		if(fuel.role == EnumCustomFuel.Role.FERTILE) return new FunctionSqrt(10);
		return null;
	}

	public static double watzPassive(EnumCustomFuel fuel) {
		if(nat(fuel) != null) return nat(fuel).watzPassive;
		// sources: about 800 base flux per unit of self influence
		return fuel.role == EnumCustomFuel.Role.SOURCE ? fuel.selfInfluence * 800D : 0D;
	}

	public static double watzMud(EnumCustomFuel fuel) {
		if(nat(fuel) != null) return nat(fuel).watzMud;
		return fuel.role == EnumCustomFuel.Role.FERTILE ? 0.0025D : 0.001D;
	}

	/// SHARED HELPERS ///

	public static double potency(CustomFuelMix mix) {
		double q = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) q += mix.fissileShareOf(fuel) * fuel.potency;
		return q <= 0 ? 1D : q;
	}

	public static double absorption(CustomFuelMix mix) {
		double a = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) a += mix.share(fuel) * fuel.absorption;
		return a;
	}

	// fertile isotopes don't cancel sources out, they only absorb flux in the hybrid model
	public static double selfInfluence(CustomFuelMix mix) {
		double self = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) self += mix.share(fuel) * Math.max(fuel.selfInfluence, 0);
		return self;
	}

	public static double hybridEnrichment(double e) {
		return e <= 0 ? 0D : Math.pow(e, HYBRID_ENRICHMENT_EXP) / e;
	}

	public static double hybridCatalysis(CustomFuelMix mix) {
		return 1D + Math.min(2D * mix.schrabidicShare(), 1D);
	}

	public static double hybridCooling(CustomFuelMix mix) {
		return 1D - 0.06D * Math.max(mix.fissileDiversity() - 1, 0);
	}

	private static double derivedHighMult(double e, double start) {
		return 0.75D + 0.25D * (e - start) / (1D - start);
	}

	private static double derivedLowScale(double e, double anchor) {
		return Math.pow(Math.max(e, 0.001D) / anchor, 0.3D);
	}

	/// RBMK ///

	public static class RBMKTerm {
		public final EnumBurnFunc func;
		public final double reactivity;
		public final double weight;
		public RBMKTerm(EnumBurnFunc func, double reactivity, double weight) { this.func = func; this.reactivity = reactivity; this.weight = weight; }
	}

	public static class RBMKProfile {
		public final List<RBMKTerm> terms = new ArrayList();
		public double fluxMult = 1D;
		public double selfRate;
		// share of source neutrons caught by the fissile isotopes, the rest leaves the rod like a vanilla Be source
		public double selfCapture;
		public double heat = 1D;
		public double meltingPoint;
		public double xenonMult = 1D;

		public double react(double flux) {
			double out = 0;
			for(RBMKTerm term : terms) out += term.weight * ItemRBMKRod.evalBurnFunc(term.func, flux * fluxMult, term.reactivity);
			return out;
		}
	}

	// depletion 0 = fresh, 1 = spent, only the hybrid model breeds in the core
	public static RBMKProfile rbmk(CustomFuelMix mix, double depletion) {
		Model model = current();
		if(model == Model.HYBRID) mix = mix.withBreeding(depletion);

		RBMKProfile p = new RBMKProfile();
		double e = mix.fissileShare();
		double melt = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) melt += mix.share(fuel) * fuel.meltingPoint;
		p.meltingPoint = melt;
		p.selfRate = selfInfluence(mix) * RBMK_SELF_SCALE;
		p.selfCapture = e;

		double nativeHeat = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) nativeHeat += mix.fissileShareOf(fuel) * rbmkHeat(fuel);

		switch(model) {
		case WEIGHTED:
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(fuel.fissile && mix.share(fuel) > 0) p.terms.add(new RBMKTerm(rbmkFunc(fuel), rbmkStats(fuel), mix.share(fuel)));
			}
			p.heat = nativeHeat;
			break;

		case DERIVED:
			double q = potency(mix);
			if(e < 0.2D) {
				p.terms.add(new RBMKTerm(EnumBurnFunc.LOG_TEN, 20D * q * derivedLowScale(e, 0.1D), 1D));
				p.heat = 0.65D * (0.8D + 0.2D * q);
			} else if(e < 0.5D) {
				p.terms.add(new RBMKTerm(EnumBurnFunc.SQUARE_ROOT, 35D * q * derivedLowScale(e, 0.35D), 1D));
				p.heat = 0.85D * (0.8D + 0.2D * q);
			} else {
				double mult = derivedHighMult(e, 0.5D);
				for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
					if(fuel.fissile && mix.share(fuel) > 0) p.terms.add(new RBMKTerm(rbmkFunc(fuel), rbmkStats(fuel) * mult, mix.fissileShareOf(fuel)));
				}
				p.heat = nativeHeat;
			}
			break;

		case HYBRID:
			double enrich = hybridEnrichment(e);
			double catalysis = hybridCatalysis(mix);
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(!fuel.fissile || mix.share(fuel) <= 0) continue;
				double weight = mix.share(fuel) * enrich * (fuel.isSchrabidic() ? 1D : catalysis);
				p.terms.add(new RBMKTerm(rbmkFunc(fuel), rbmkStats(fuel), weight));
			}
			p.fluxMult = 1D - absorption(mix);
			p.heat = nativeHeat * hybridCooling(mix) * (1D + 0.5D * mix.schrabidicShare());
			p.xenonMult = 1D - 0.8D * mix.share(EnumCustomFuel.TH232);
			break;
		}

		return p;
	}

	/// PWR ///

	public static class FuncTerm {
		public final Function func;
		public final double weight;
		public FuncTerm(Function func, double weight) { this.func = func; this.weight = weight; }
	}

	public static class PWRProfile {
		public final List<FuncTerm> terms = new ArrayList();
		public double fluxMult = 1D;
		public double heat;
		public static final double BASE_YIELD = 1_000_000_000D;
		public double yield = BASE_YIELD;
		public double selfFlux;

		public double react(double flux) {
			double out = 0;
			for(FuncTerm term : terms) out += term.weight * term.func.effonix(flux * fluxMult);
			return out;
		}
	}

	public static PWRProfile pwr(CustomFuelMix mix, double depletion) {
		Model model = current();
		if(model == Model.HYBRID) mix = mix.withBreeding(depletion);

		PWRProfile p = new PWRProfile();
		double e = mix.fissileShare();
		p.selfFlux = selfInfluence(mix) * PWR_SELF_SCALE;
		double nativeHeat = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) nativeHeat += mix.fissileShareOf(fuel) * pwrHeat(fuel);

		switch(model) {
		case WEIGHTED:
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(fuel.fissile && mix.share(fuel) > 0) p.terms.add(new FuncTerm(pwrFunc(fuel), mix.share(fuel)));
			}
			p.heat = nativeHeat;
			break;

		case DERIVED:
			double q = potency(mix);
			if(e < 0.3D) {
				p.terms.add(new FuncTerm(new FunctionLogarithmic(600D * q * derivedLowScale(e, 0.1D)).withDiv(2_500), 1D));
				p.heat = 5D + 2.5D * (q - 1D);
			} else {
				double mult = derivedHighMult(e, 0.3D);
				for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
					if(fuel.fissile && mix.share(fuel) > 0) p.terms.add(new FuncTerm(pwrFunc(fuel), mix.fissileShareOf(fuel) * mult));
				}
				p.heat = nativeHeat;
			}
			break;

		case HYBRID:
			double enrich = hybridEnrichment(e);
			double catalysis = hybridCatalysis(mix);
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(!fuel.fissile || mix.share(fuel) <= 0) continue;
				p.terms.add(new FuncTerm(pwrFunc(fuel), mix.share(fuel) * enrich * (fuel.isSchrabidic() ? 1D : catalysis)));
			}
			p.fluxMult = 1D - absorption(mix);
			p.heat = nativeHeat * hybridCooling(mix) * (1D + 0.5D * mix.schrabidicShare());
			// fertile-rich fuel lasts longer since it keeps breeding
			p.yield *= 1D + absorption(mix);
			break;
		}

		return p;
	}

	/// ZIRNOX ///

	public static class ZirnoxProfile {
		public int heat;
		public int maxLife;
		// no fissile material: breeds from its neighbours like the vanilla thorium rod
		public boolean breeding;
	}

	public static ZirnoxProfile zirnox(CustomFuelMix mix, double depletion) {
		Model model = current();
		if(model == Model.HYBRID) mix = mix.withBreeding(depletion);

		ZirnoxProfile p = new ZirnoxProfile();
		double e = mix.fissileShare();
		p.breeding = e < 0.01D;
		if(p.breeding) {
			p.maxLife = 20_000;
			return p;
		}

		double nativeHeat = 0, nativeLife = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			nativeHeat += mix.fissileShareOf(fuel) * zirnoxHeat(fuel);
			nativeLife += mix.fissileShareOf(fuel) * zirnoxLife(fuel);
		}

		double heat = 0, life = nativeLife;

		switch(model) {
		case WEIGHTED:
			heat = nativeHeat * e;
			break;

		case DERIVED:
			double q = potency(mix);
			if(e < 0.5D) {
				heat = 50D * q * Math.pow(e / 0.05D, 0.35D);
				life = 250_000D + (nativeLife - 250_000D) * (e / 0.5D);
			} else {
				heat = nativeHeat * derivedHighMult(e, 0.5D);
			}
			break;

		case HYBRID:
			heat = nativeHeat * Math.pow(e, HYBRID_ENRICHMENT_EXP) * hybridCatalysis(mix) * hybridCooling(mix) * (1D - 0.5D * absorption(mix));
			life = nativeLife * (1D + absorption(mix));
			break;
		}

		p.heat = Math.max((int) Math.round(heat), 1);
		p.maxLife = Math.max((int) Math.round(life), 1);
		return p;
	}

	/// WATZ ///

	public static class WatzProfile {
		public final List<FuncTerm> burn = new ArrayList();
		public final List<FuncTerm> heatDiv = new ArrayList();
		public final List<FuncTerm> absorb = new ArrayList();
		public double passive;
		public double heatEmission;
		public double mudContent;

		public double burn(double flux, double heat) {
			double out = 0;
			for(FuncTerm term : burn) out += term.weight * term.func.effonix(flux);
			return out / heatDivisor(heat);
		}

		public double heatDivisor(double heat) {
			double div = 0, weight = 0;
			for(FuncTerm term : heatDiv) { div += term.weight * term.func.effonix(heat); weight += term.weight; }
			return weight <= 0 ? 1D : Math.max(div / weight, 0.01D);
		}

		public double absorb(double flux) {
			double out = 0;
			for(FuncTerm term : absorb) out += term.weight * term.func.effonix(flux);
			return out;
		}
	}

	public static WatzProfile watz(CustomFuelMix mix, double depletion) {
		Model model = current();
		if(model == Model.HYBRID) mix = mix.withBreeding(depletion);

		WatzProfile p = new WatzProfile();
		double e = mix.fissileShare();
		double nativeHeat = 0, passive = 0, mud = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			nativeHeat += mix.fissileShareOf(fuel) * watzHeat(fuel);
			passive += mix.share(fuel) * watzPassive(fuel);
			mud += mix.share(fuel) * watzMud(fuel);
			if(fuel.fissile && mix.share(fuel) > 0) p.heatDiv.add(new FuncTerm(watzHeatDiv(fuel), mix.fissileShareOf(fuel)));
			Function absorb = watzAbsorb(fuel);
			if(absorb != null && mix.share(fuel) > 0) p.absorb.add(new FuncTerm(absorb, mix.share(fuel)));
		}
		p.passive = passive;
		p.mudContent = mud / 2D;
		p.heatEmission = nativeHeat;

		switch(model) {
		case WEIGHTED:
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(fuel.fissile && mix.share(fuel) > 0) p.burn.add(new FuncTerm(watzBurn(fuel), mix.share(fuel)));
			}
			break;

		case DERIVED:
			double q = potency(mix);
			if(e < 0.3D) {
				p.burn.add(new FuncTerm(new FunctionSqrt(75D * q * derivedLowScale(e, 0.1D)), 1D));
				p.heatEmission = 10D;
			} else {
				double mult = derivedHighMult(e, 0.3D);
				for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
					if(fuel.fissile && mix.share(fuel) > 0) p.burn.add(new FuncTerm(watzBurn(fuel), mix.fissileShareOf(fuel) * mult));
				}
			}
			break;

		case HYBRID:
			double enrich = hybridEnrichment(e);
			double catalysis = hybridCatalysis(mix);
			for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if(!fuel.fissile || mix.share(fuel) <= 0) continue;
				p.burn.add(new FuncTerm(watzBurn(fuel), mix.share(fuel) * enrich * (fuel.isSchrabidic() ? 1D : catalysis)));
			}
			p.heatEmission *= hybridCooling(mix) * (1D + 0.5D * mix.schrabidicShare());
			p.mudContent *= 1D - 0.5D * mix.share(EnumCustomFuel.TH232);
			break;
		}

		return p;
	}
}
