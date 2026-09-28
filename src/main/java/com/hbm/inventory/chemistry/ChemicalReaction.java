package com.hbm.inventory.chemistry;

import java.util.LinkedHashMap;
import java.util.Map;

/** A deliberately small implementation of the original FermiChem equilibrium model. */
public class ChemicalReaction {

	public final String id;
	public final String translationKey;
	private final Map<ChemReagent, Double> reactants = new LinkedHashMap<ChemReagent, Double>();
	private final Map<ChemReagent, Double> catalysts = new LinkedHashMap<ChemReagent, Double>();
	private final Map<ChemReagent, Double> products = new LinkedHashMap<ChemReagent, Double>();

	private double requiredTemperature = 0;
	private double optimalTemperature = 293.15D;
	private double overheatTemperature = Double.MAX_VALUE;
	private double optimalPHMin = 5;
	private double optimalPHMax = 9;
	private double determiningPHRange = 4;
	private double ratePerSecond = 2;
	private double thermicConstant = 0;
	private double hydrogenIonRelease = 0;

	public ChemicalReaction(String id, String translationKey) {
		this.id = id;
		this.translationKey = translationKey;
	}

	public ChemicalReaction reactant(ChemReagent reagent, double amount) { reactants.put(reagent, amount); return this; }
	public ChemicalReaction catalyst(ChemReagent reagent, double amount) { catalysts.put(reagent, amount); return this; }
	public ChemicalReaction product(ChemReagent reagent, double amount) { products.put(reagent, amount); return this; }
	public ChemicalReaction temperature(double required, double optimal, double overheat) { requiredTemperature = required; optimalTemperature = optimal; overheatTemperature = overheat; return this; }
	public ChemicalReaction ph(double optimalMin, double optimalMax, double determiningRange) { optimalPHMin = optimalMin; optimalPHMax = optimalMax; determiningPHRange = determiningRange; return this; }
	public ChemicalReaction rate(double unitsPerSecond) { ratePerSecond = unitsPerSecond; return this; }
	public ChemicalReaction thermic(double kelvinPerUnit) { thermicConstant = kelvinPerUnit; return this; }
	public ChemicalReaction hydrogenIonRelease(double phPerUnit) { hydrogenIonRelease = phPerUnit; return this; }

	public boolean canReact(ReagentHolder holder) {
		if(holder.getTemperature() < requiredTemperature) return false;
		if(holder.getPH() < optimalPHMin - determiningPHRange || holder.getPH() > optimalPHMax + determiningPHRange) return false;
		for(Map.Entry<ChemReagent, Double> entry : reactants.entrySet()) if(!holder.has(entry.getKey(), entry.getValue() * 0.001D)) return false;
		for(Map.Entry<ChemReagent, Double> entry : catalysts.entrySet()) if(!holder.has(entry.getKey(), entry.getValue())) return false;
		return !reactants.isEmpty() && !products.isEmpty();
	}

	/** Processes one game tick and returns the amount of product made. */
	public double process(ReagentHolder holder) {
		if(!canReact(holder)) return 0;

		double batches = Double.MAX_VALUE;
		for(Map.Entry<ChemReagent, Double> entry : reactants.entrySet()) {
			batches = Math.min(batches, holder.getAmount(entry.getKey()) / entry.getValue());
		}

		double speed = temperatureRate(holder.getTemperature());
		double stepBatches = Math.min(batches, (ratePerSecond / 20.0D) * speed);
		if(stepBatches < 0.0001D) return 0;

		double reactantPurity = 0;
		for(ChemReagent reagent : reactants.keySet()) reactantPurity += holder.getPurity(reagent);
		reactantPurity /= reactants.size();
		double productPurity = clamp01(phQuality(holder.getPH()) * reactantPurity);

		for(Map.Entry<ChemReagent, Double> entry : reactants.entrySet()) holder.remove(entry.getKey(), entry.getValue() * stepBatches);

		double produced = 0;
		for(Map.Entry<ChemReagent, Double> entry : products.entrySet()) {
			double amount = entry.getValue() * stepBatches;
			produced += holder.addReactionProduct(entry.getKey(), amount, productPurity);
		}

		holder.changeTemperature(thermicConstant * produced);
		holder.changePH(hydrogenIonRelease * produced);
		return produced;
	}

	private double temperatureRate(double temperature) {
		if(temperature >= overheatTemperature) return 0.1D;
		if(temperature >= optimalTemperature) return 1.0D;
		double span = Math.max(1, optimalTemperature - requiredTemperature);
		double normalized = (temperature - requiredTemperature) / span;
		return Math.max(0.05D, normalized * normalized);
	}

	private double phQuality(double ph) {
		if(ph >= optimalPHMin && ph <= optimalPHMax) return 1;
		if(ph < optimalPHMin) return clamp01((ph - (optimalPHMin - determiningPHRange)) / determiningPHRange);
		return clamp01(((optimalPHMax + determiningPHRange) - ph) / determiningPHRange);
	}

	private static double clamp01(double value) { return Math.max(0, Math.min(1, value)); }
}
