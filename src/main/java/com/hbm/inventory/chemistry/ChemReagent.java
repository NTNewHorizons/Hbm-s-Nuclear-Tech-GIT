package com.hbm.inventory.chemistry;

/**
 * A chemical datum in the small SS13-style reagent system.
 *
 * IDs are serialized, so they must remain stable. Reagents are deliberately
 * data-only; behavior belongs to reactions and holders.
 */
public class ChemReagent {

	public final String id;
	public final String translationKey;
	public final int color;
	public final double ph;
	private ChemReagent inverse;
	private double inversePurity = 0.25D;

	public ChemReagent(String id, String translationKey, int color, double ph) {
		this.id = id;
		this.translationKey = translationKey;
		this.color = color;
		this.ph = ph;
	}

	public ChemReagent inverse(ChemReagent inverse, double threshold) {
		this.inverse = inverse;
		this.inversePurity = threshold;
		return this;
	}

	public ChemReagent getInverse() {
		return inverse;
	}

	public double getInversePurity() {
		return inversePurity;
	}
}
