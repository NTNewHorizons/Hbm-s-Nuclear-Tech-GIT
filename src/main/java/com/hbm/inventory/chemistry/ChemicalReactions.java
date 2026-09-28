package com.hbm.inventory.chemistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChemicalReactions {

	private static final List<ChemicalReaction> REACTIONS = new ArrayList<ChemicalReaction>();

	static {
		register(new ChemicalReaction("water_synthesis", "chem.reaction.ss13.water")
				.reactant(ChemReagents.HYDROGEN, 1).reactant(ChemReagents.OXYGEN, 1)
				.product(ChemReagents.WATER, 2).temperature(273.15D, 293.15D, 650D)
				.ph(5, 9, 4).rate(2).thermic(0.08D));

		register(new ChemicalReaction("haber_test", "chem.reaction.ss13.ammonia")
				.reactant(ChemReagents.HYDROGEN, 3).reactant(ChemReagents.NITROGEN, 1)
				.catalyst(ChemReagents.IRON_CATALYST, 1).product(ChemReagents.AMMONIA, 4)
				.temperature(273.15D, 293.15D, 750D).ph(5, 9, 4).rate(1.5D).thermic(0.04D));

		register(new ChemicalReaction("salt_synthesis", "chem.reaction.ss13.salt")
				.reactant(ChemReagents.SODIUM, 1).reactant(ChemReagents.CHLORINE, 1)
				.product(ChemReagents.SALT, 2).temperature(273.15D, 310D, 700D)
				.ph(4, 10, 4).rate(2).thermic(0.15D));

		register(new ChemicalReaction("space_cleaner", "chem.reaction.ss13.space_cleaner")
				.reactant(ChemReagents.AMMONIA, 1).reactant(ChemReagents.WATER, 1)
				.product(ChemReagents.SPACE_CLEANER, 2).temperature(273.15D, 293.15D, 500D)
				.ph(8, 10, 3).rate(2).hydrogenIonRelease(-0.01D));
	}

	private ChemicalReactions() { }

	private static void register(ChemicalReaction reaction) { REACTIONS.add(reaction); }

	public static List<ChemicalReaction> all() { return Collections.unmodifiableList(REACTIONS); }

	/** Runs the first matching reaction, mirroring SS13's reaction selection order. */
	public static ChemicalReaction process(ReagentHolder holder) {
		for(ChemicalReaction reaction : REACTIONS) {
			if(reaction.process(holder) > 0) return reaction;
		}
		return null;
	}
}
