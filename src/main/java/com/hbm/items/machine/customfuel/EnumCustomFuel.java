package com.hbm.items.machine.customfuel;

import com.hbm.hazard.HazardRegistry;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;

import net.minecraft.item.Item;

public enum EnumCustomFuel {

	//			material				role		potency	self	absorb	melt	rad
	TH232(		Mats.MAT_THORIUM,		Role.FERTILE,	0D,		-0.15D,	0.25D,	3350,	HazardRegistry.th232),
	U233(		Mats.MAT_U233,			Role.FISSILE,	1.15D,	0.10D,	0D,		2865,	HazardRegistry.u233),
	U235(		Mats.MAT_U235,			Role.FISSILE,	1.00D,	0.05D,	0D,		2865,	HazardRegistry.u235),
	U238(		Mats.MAT_U238,			Role.FERTILE,	0D,		-0.25D,	0.35D,	2865,	HazardRegistry.u238),
	PU238(		Mats.MAT_PU238,			Role.SOURCE,	0D,		1.00D,	0D,		1287,	HazardRegistry.pu238),
	PU239(		Mats.MAT_PU239,			Role.FISSILE,	1.60D,	0.10D,	0D,		2744,	HazardRegistry.pu239),
	PU240(		Mats.MAT_PU240,			Role.FERTILE,	0D,		-0.20D,	0.30D,	2744,	HazardRegistry.pu240),
	PU241(		Mats.MAT_PU241,			Role.FISSILE,	1.90D,	0.10D,	0D,		2744,	HazardRegistry.pu241),
	NP237(		Mats.MAT_NEPTUNIUM,		Role.FISSILE,	1.30D,	0.05D,	0D,		2800,	HazardRegistry.np237),
	AM241(		Mats.MAT_AM241,			Role.FERTILE,	0D,		-0.20D,	0.30D,	2386,	HazardRegistry.am241),
	AM242(		Mats.MAT_AM242,			Role.FISSILE,	2.00D,	0.10D,	0D,		2386,	HazardRegistry.am242),
	CM242(		Mats.MAT_CM242,			Role.SOURCE,	0D,		0.75D,	0D,		1880,	HazardRegistry.cm242),
	CM243(		Mats.MAT_CM243,			Role.FISSILE,	2.00D,	0.15D,	0D,		1880,	HazardRegistry.cm243),
	CM244(		Mats.MAT_CM244,			Role.FERTILE,	0D,		-0.10D,	0.25D,	1880,	HazardRegistry.cm244),
	CM245(		Mats.MAT_CM245,			Role.FISSILE,	2.20D,	0.15D,	0D,		1880,	HazardRegistry.cm245),
	CM246(		Mats.MAT_CM246,			Role.FERTILE,	0D,		-0.10D,	0.25D,	1880,	HazardRegistry.cm246),
	CM247(		Mats.MAT_CM247,			Role.FISSILE,	2.30D,	0.15D,	0D,		1880,	HazardRegistry.cm247),
	BK247(		Mats.MAT_BK247,			Role.FISSILE,	2.40D,	0.20D,	0D,		2993,	HazardRegistry.bk247),
	CF251(		Mats.MAT_CF251,			Role.FISSILE,	2.60D,	0.25D,	0D,		2000,	HazardRegistry.cf251),
	CF252(		Mats.MAT_CF252,			Role.SOURCE,	0D,		1.50D,	0D,		2000,	HazardRegistry.cf252),
	ES253(		Mats.MAT_ES253,			Role.FISSILE,	2.80D,	0.30D,	0D,		1800,	HazardRegistry.es253),
	ES255(		Mats.MAT_ES255,			Role.SOURCE,	0D,		0.60D,	0D,		1800,	HazardRegistry.es255),
	PO210(		Mats.MAT_POLONIUM,		Role.SOURCE,	0D,		1.25D,	0D,		1287,	HazardRegistry.po210),
	RA226(		Mats.MAT_RADIUM,		Role.SOURCE,	0D,		0.50D,	0D,		700,	HazardRegistry.ra226),
	SA326(		Mats.MAT_SCHRABIDIUM,	Role.FISSILE,	2.60D,	0.20D,	0D,		3000,	HazardRegistry.sa326),
	SA327(		Mats.MAT_SOLINIUM,		Role.FISSILE,	2.30D,	0.50D,	0D,		2750,	HazardRegistry.sa327),
	AUS(		Mats.MAT_AUSTRALIUM,	Role.FISSILE,	1.80D,	0.00D,	0D,		5211,	1F);

	// SOURCE isotopes don't split, they only emit neutrons (like the vanilla Be sources)
	public static enum Role { FISSILE, FERTILE, SOURCE }

	public final NTMMaterial stack;
	public final Role role;
	public final boolean fissile;
	// strength relative to U-235, derived model
	public final double potency;
	// neutrons emitted (positive) or swallowed (negative) without incoming flux
	public final double selfInfluence;
	// share of incoming flux eaten by pure fertile fuel, hybrid model
	public final double absorption;
	public final double meltingPoint;
	// per ingot
	public final float radiation;

	private EnumCustomFuel(NTMMaterial stack, Role role, double potency, double self, double absorb, double melt, float rad) {
		this.stack = stack;
		this.role = role;
		this.fissile = role == Role.FISSILE;
		this.potency = potency;
		this.selfInfluence = self;
		this.absorption = absorb;
		this.meltingPoint = melt;
		this.radiation = rad;
	}

	public EnumCustomFuel breedsInto() {
		switch(this) {
		case U238: return PU239;
		case TH232: return U233;
		case PU240: return PU241;
		case AM241: return AM242;
		case CM244: return CM245;
		case CM246: return CM247;
		default: return null;
		}
	}

	public boolean isSchrabidic() {
		return this == SA326 || this == SA327;
	}

	// null when the mod has no nugget for it, PUREX then counts it as waste
	public Item nugget() {
		switch(this) {
		case TH232: return ModItems.nugget_th232;
		case U233: return ModItems.nugget_u233;
		case U235: return ModItems.nugget_u235;
		case U238: return ModItems.nugget_u238;
		case PU238: return ModItems.nugget_pu238;
		case PU239: return ModItems.nugget_pu239;
		case PU240: return ModItems.nugget_pu240;
		case PU241: return ModItems.nugget_pu241;
		case NP237: return ModItems.nugget_neptunium;
		case AM241: return ModItems.nugget_am241;
		case AM242: return ModItems.nugget_am242;
		case CM242: return ModItems.nugget_cm242;
		case CM243: return ModItems.nugget_cm243;
		case CM244: return ModItems.nugget_cm244;
		case CM245: return ModItems.nugget_cm245;
		case CM246: return ModItems.nugget_cm246;
		case CM247: return ModItems.nugget_cm247;
		case BK247: return ModItems.nugget_bk247;
		case CF251: return ModItems.nugget_cf251;
		case CF252: return ModItems.nugget_cf252;
		case ES253: return ModItems.nugget_es253;
		case PO210: return ModItems.nugget_polonium;
		case RA226: return ModItems.nugget_ra226;
		case SA326: return ModItems.nugget_schrabidium;
		case SA327: return ModItems.nugget_solinium;
		case AUS: return ModItems.nugget_australium;
		default: return null;
		}
	}

	public static EnumCustomFuel byMaterial(NTMMaterial mat) {
		if(mat == null) return null;
		for(EnumCustomFuel fuel : values()) if(fuel.stack == mat) return fuel;
		return null;
	}
}
