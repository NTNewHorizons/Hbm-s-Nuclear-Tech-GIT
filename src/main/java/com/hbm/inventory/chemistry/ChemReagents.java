package com.hbm.inventory.chemistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChemReagents {

	private static final Map<String, ChemReagent> REGISTRY = new LinkedHashMap<String, ChemReagent>();
	private static final List<ChemReagent> DISPENSABLE = new ArrayList<ChemReagent>();

	public static final ChemReagent TOXIC_SLURRY = register(new ChemReagent("toxic_slurry", "chem.ss13.toxic_slurry", 0x667A32, 2.0D));
	public static final ChemReagent HYDROGEN = dispense(register(new ChemReagent("hydrogen", "chem.ss13.hydrogen", 0xD9F3FF, 7.0D)));
	public static final ChemReagent OXYGEN = dispense(register(new ChemReagent("oxygen", "chem.ss13.oxygen", 0x8FD8FF, 7.0D)));
	public static final ChemReagent NITROGEN = dispense(register(new ChemReagent("nitrogen", "chem.ss13.nitrogen", 0xB6BCE8, 7.0D)));
	public static final ChemReagent SODIUM = dispense(register(new ChemReagent("sodium", "chem.ss13.sodium", 0xD9D9D9, 12.0D)));
	public static final ChemReagent CHLORINE = dispense(register(new ChemReagent("chlorine", "chem.ss13.chlorine", 0xC8D43A, 3.0D)));
	public static final ChemReagent IRON_CATALYST = dispense(register(new ChemReagent("iron_catalyst", "chem.ss13.iron_catalyst", 0x8A6755, 7.0D)));

	public static final ChemReagent WATER = register(new ChemReagent("water", "chem.ss13.water", 0x4D9BE6, 7.0D)).inverse(TOXIC_SLURRY, 0.25D);
	public static final ChemReagent AMMONIA = register(new ChemReagent("ammonia", "chem.ss13.ammonia", 0xB8D8A8, 11.5D)).inverse(TOXIC_SLURRY, 0.25D);
	public static final ChemReagent SALT = register(new ChemReagent("salt", "chem.ss13.salt", 0xF1F1E6, 7.0D)).inverse(TOXIC_SLURRY, 0.25D);
	public static final ChemReagent SPACE_CLEANER = register(new ChemReagent("space_cleaner", "chem.ss13.space_cleaner", 0xA8E7E9, 9.0D)).inverse(TOXIC_SLURRY, 0.25D);

	private ChemReagents() { }

	private static ChemReagent register(ChemReagent reagent) {
		if(REGISTRY.containsKey(reagent.id)) throw new IllegalArgumentException("Duplicate chemical reagent: " + reagent.id);
		REGISTRY.put(reagent.id, reagent);
		return reagent;
	}

	private static ChemReagent dispense(ChemReagent reagent) {
		DISPENSABLE.add(reagent);
		return reagent;
	}

	public static ChemReagent get(String id) {
		return REGISTRY.get(id);
	}

	public static Collection<ChemReagent> all() {
		return Collections.unmodifiableCollection(REGISTRY.values());
	}

	public static List<ChemReagent> dispensable() {
		return Collections.unmodifiableList(DISPENSABLE);
	}
}
