package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.item.ItemStack;

public class GasCentrifugeRecipes extends SerializableRecipe {

	private static Map<String, PseudoFluidType> stages = new HashMap();

	@Override public String getFileName() { return "hbmGasCentrifuge.json"; }
	@Override public void registerDefaults() { register(); }
	@Override public String getComment() {
		return "Display entries only affect NEI. Stages can be defined in any order, outputStage NONE ends the cascade. terminalAmount and terminalOutputs are optional, they turn that amount of output fluid into items when there is no next centrifuge. Stage names are localized using hbmpseudofluid.<lowercase name>.";
	}

	@Override
	public Object getRecipeObject() {
		ArrayList<Object> entries = new ArrayList();
		entries.addAll(stages.values());
		entries.addAll(fluidConversions.entrySet());
		entries.addAll(gasCent.entrySet());
		return entries;
	}

	@Override
	public void deleteRecipes() {
		// Keep identities stable: loaded tanks and built-in constants reference these objects.
		for(PseudoFluidType type : PseudoFluidType.types.values()) {
			type.fluidConsumed = 0;
			type.fluidProduced = 0;
			type.outputFluid = PseudoFluidType.NONE;
			type.isHighSpeed = false;
			type.output = null;
			type.terminalAmount = 0;
			type.terminalOutputs = new ItemStack[0];
		}
		stages.clear();
		fluidConversions.clear();
		gasCent.clear();
	}

	private static PseudoFluidType getOrCreateStage(String name) {
		PseudoFluidType type = PseudoFluidType.types.get(name);
		return type != null ? type : new PseudoFluidType(name);
	}

	private static void addStage(PseudoFluidType type, int consumed, int produced, PseudoFluidType outputType, boolean highSpeed, ItemStack... outputs) {
		type.fluidConsumed = consumed;
		type.fluidProduced = produced;
		type.outputFluid = outputType;
		type.isHighSpeed = highSpeed;
		type.output = outputs;
		stages.put(type.name, type);
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = recipe.getAsJsonObject();
		String type = obj.get("type").getAsString();
		if("stage".equals(type)) {
			String name = obj.get("name").getAsString();
			if("NONE".equals(name) || name.isEmpty()) throw new IllegalArgumentException("Invalid gas centrifuge stage name");
			int consumed = obj.get("fluidConsumed").getAsInt();
			int produced = obj.get("fluidProduced").getAsInt();
			ItemStack[] outputs = readItemStackArray(obj.getAsJsonArray("outputs"));
			if(consumed <= 0 || produced < 0) throw new IllegalArgumentException("Gas centrifuge fluidConsumed must be positive and fluidProduced must be nonnegative");
			PseudoFluidType stage = getOrCreateStage(name);
			addStage(stage, consumed, produced, getOrCreateStage(obj.get("outputStage").getAsString()), obj.get("highSpeed").getAsBoolean(), outputs);
			stage.terminalAmount = obj.has("terminalAmount") ? obj.get("terminalAmount").getAsInt() : 0;
			stage.terminalOutputs = obj.has("terminalOutputs") ? readItemStackArray(obj.getAsJsonArray("terminalOutputs")) : new ItemStack[0];
			if(stage.terminalAmount < 0) throw new IllegalArgumentException("Gas centrifuge terminalAmount must be nonnegative");
		} else if("conversion".equals(type)) {
			fluidConversions.put(Fluids.fromName(obj.get("input").getAsString()), getOrCreateStage(obj.get("stage").getAsString()));
		} else if("display".equals(type)) {
			ItemStack[] outputs = readItemStackArray(obj.getAsJsonArray("outputs"));
			int count = obj.get("centrifuges").getAsInt();
			if(outputs.length > 4 || count <= 0) throw new IllegalArgumentException("Invalid gas centrifuge display recipe");
			gasCent.put(readFluidStack(obj.getAsJsonArray("input")), new Object[] {outputs, obj.get("highSpeed").getAsBoolean(), count});
		} else {
			throw new IllegalArgumentException("Unknown gas centrifuge entry type: " + type);
		}
	}

	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		if(recipe instanceof PseudoFluidType) {
			PseudoFluidType stage = (PseudoFluidType) recipe;
			writer.name("type").value("stage");
			writer.name("name").value(stage.name);
			writer.name("fluidConsumed").value(stage.fluidConsumed);
			writer.name("fluidProduced").value(stage.fluidProduced);
			writer.name("outputStage").value(stage.outputFluid.name);
			writer.name("highSpeed").value(stage.isHighSpeed);
			writer.name("outputs").beginArray();
			for(ItemStack output : stage.output) writeItemStack(output, writer);
			writer.endArray();
			if(stage.terminalAmount > 0) {
				writer.name("terminalAmount").value(stage.terminalAmount);
				writer.name("terminalOutputs").beginArray();
				for(ItemStack output : stage.terminalOutputs) writeItemStack(output, writer);
				writer.endArray();
			}
		} else {
			Entry<?, ?> entry = (Entry<?, ?>) recipe;
			if(entry.getKey() instanceof FluidType) {
				writer.name("type").value("conversion");
				writer.name("input").value(((FluidType) entry.getKey()).getName());
				writer.name("stage").value(((PseudoFluidType) entry.getValue()).name);
			} else {
				Object[] display = (Object[]) entry.getValue();
				writer.name("type").value("display");
				writer.name("input"); writeFluidStack((FluidStack) entry.getKey(), writer);
				writer.name("outputs").beginArray();
				for(ItemStack output : (ItemStack[]) display[0]) writeItemStack(output, writer);
				writer.endArray();
				writer.name("highSpeed").value((Boolean) display[1]);
				writer.name("centrifuges").value((Integer) display[2]);
			}
		}
	}

	public static class PseudoFluidType {

		public static HashMap<String, PseudoFluidType> types = new HashMap();

		public static PseudoFluidType NONE		= new PseudoFluidType("NONE",		0,		0,		null,		false,	(ItemStack[])null);

		public static PseudoFluidType HEUF6 = new PseudoFluidType("HEUF6");
		public static PseudoFluidType MEUF6 = new PseudoFluidType("MEUF6");
		public static PseudoFluidType LEUF6 = new PseudoFluidType("LEUF6");
		public static PseudoFluidType NUF6 = new PseudoFluidType("NUF6");

		public static PseudoFluidType PF6 = new PseudoFluidType("PF6");

		public static PseudoFluidType MUD_HEAVY = new PseudoFluidType("MUD_HEAVY");
		public static PseudoFluidType MUD = new PseudoFluidType("MUD");

		public static PseudoFluidType MINSOLSEP = new PseudoFluidType("MINSOLSEP");
		public static PseudoFluidType MINSOL = new PseudoFluidType("MINSOL");


		
		public String name;
		int fluidConsumed;
		int fluidProduced;
		PseudoFluidType outputFluid;
		boolean isHighSpeed;
		ItemStack[] output;
		int terminalAmount;
		ItemStack[] terminalOutputs = new ItemStack[0];

		PseudoFluidType(String name) {
			this(name, 0, 0, NONE, false, (ItemStack[]) null);
		}

		PseudoFluidType(String name, int fluidConsumed, int fluidProduced, PseudoFluidType outputFluid, boolean isHighSpeed, ItemStack... output) {
			this.name = name;
			this.fluidConsumed = fluidConsumed;
			this.fluidProduced = fluidProduced;
			this.outputFluid = outputFluid;
			this.isHighSpeed = isHighSpeed;
			this.output = output;
			types.put(name, this);
		}

		public int getFluidConsumed() {				return this.fluidConsumed; }
		public int getFluidProduced() {				return this.fluidProduced; }
		public PseudoFluidType getOutputType() {	return this.outputFluid != null ? this.outputFluid : NONE; }
		public ItemStack[] getOutput() {			return this.output; }
		public boolean getIfHighSpeed() {			return this.isHighSpeed; }
		public int getTerminalAmount() {			return this.terminalAmount; }
		public ItemStack[] getTerminalOutputs() {	return this.terminalOutputs; }
		public String getName() {					return I18nUtil.resolveKey("hbmpseudofluid.".concat(this.name.toLowerCase(Locale.US))); }

	}

	/* Recipe NEI Handler */
	//Fluid input; ItemStack[] outputs, isHighSpeed, # of centrifuges
	private static Map<FluidStack, Object[]> gasCent = new HashMap();

	//Iterators are lots of fun
	public static Map<Object, Object[]> getGasCentrifugeRecipes() {
		Map<Object, Object[]> recipes = new HashMap<Object, Object[]>();
		Iterator itr = gasCent.entrySet().iterator();

		while(itr.hasNext()) {
			Map.Entry<Object, Object[]> entry = (Entry) itr.next();
			FluidStack input = (FluidStack) entry.getKey();
			ItemStack[] out = new ItemStack[4];
			ItemStack[] outputs = (ItemStack[]) entry.getValue()[0];

			for(int j = 0; j < outputs.length; j++) {
				out[j] = outputs[j].copy();
			}
			for(int j = 0; j < 4; j++)
				if(out[j] == null)
					out[j] = new ItemStack(ModItems.nothing);

			recipes.put(ItemFluidIcon.make(input.type, input.fill), new Object[] { out, entry.getValue()[1], entry.getValue()[2] });
		}

		return recipes;
	}

	public static HashMap<FluidType, PseudoFluidType> fluidConversions = new HashMap();

	public static void register() {
		addStage(PseudoFluidType.HEUF6, 300, 0, PseudoFluidType.NONE, true, new ItemStack(ModItems.nugget_u238, 2), new ItemStack(ModItems.nugget_u235, 1), new ItemStack(ModItems.fluorite, 1));
		addStage(PseudoFluidType.MEUF6, 200, 100, PseudoFluidType.HEUF6, false, new ItemStack(ModItems.nugget_u238, 1));
		addStage(PseudoFluidType.LEUF6, 300, 200, PseudoFluidType.MEUF6, false, new ItemStack(ModItems.nugget_u238, 1), new ItemStack(ModItems.fluorite, 1));
		addStage(PseudoFluidType.NUF6, 400, 300, PseudoFluidType.LEUF6, false, new ItemStack(ModItems.nugget_u238, 1));
		addStage(PseudoFluidType.PF6, 300, 0, PseudoFluidType.NONE, false, new ItemStack(ModItems.nugget_pu238, 1), new ItemStack(ModItems.nugget_pu_mix, 2), new ItemStack(ModItems.fluorite, 1));
		addStage(PseudoFluidType.MUD_HEAVY, 500, 0, PseudoFluidType.NONE, false, new ItemStack(ModItems.powder_iron, 1), new ItemStack(ModItems.dust, 1), new ItemStack(ModItems.nuclear_waste_tiny, 1));
		addStage(PseudoFluidType.MUD, 1000, 500, PseudoFluidType.MUD_HEAVY, false, new ItemStack(ModItems.powder_lead, 1), new ItemStack(ModItems.dust, 1));
		addStage(PseudoFluidType.MINSOLSEP, 500, 0, PseudoFluidType.NONE, false, new ItemStack(ModItems.crystal_cleaned, 1));
		addStage(PseudoFluidType.MINSOL, 500, 500, PseudoFluidType.MINSOLSEP, false, new ItemStack(ModItems.powder_iron, 1));
		PseudoFluidType.LEUF6.terminalAmount = 600;
		PseudoFluidType.LEUF6.terminalOutputs = new ItemStack[] {new ItemStack(ModItems.nugget_uranium_fuel, 6), new ItemStack(ModItems.fluorite)};

		fluidConversions.put(Fluids.UF6, PseudoFluidType.NUF6);
		fluidConversions.put(Fluids.PUF6, PseudoFluidType.PF6);
		fluidConversions.put(Fluids.WATZ, PseudoFluidType.MUD);
		fluidConversions.put(Fluids.MINSOL, PseudoFluidType.MINSOL);

		gasCent.put(new FluidStack(1200, Fluids.UF6), new Object[] { new ItemStack[] {new ItemStack(ModItems.nugget_u238, 11), new ItemStack(ModItems.nugget_u235, 1), new ItemStack(ModItems.fluorite, 4)}, true, 4 });
		gasCent.put(new FluidStack(1200, Fluids.UF6), new Object[] { new ItemStack[] {new ItemStack(ModItems.nugget_u238, 6), new ItemStack(ModItems.nugget_uranium_fuel, 6), new ItemStack(ModItems.fluorite, 4)}, false, 2 });
		gasCent.put(new FluidStack(900, Fluids.PUF6), new Object[] { new ItemStack[] {new ItemStack(ModItems.nugget_pu238, 3), new ItemStack(ModItems.nugget_pu_mix, 6), new ItemStack(ModItems.fluorite, 3)}, false, 1 });
		gasCent.put(new FluidStack(1000, Fluids.WATZ), new Object[] { new ItemStack[] {new ItemStack(ModItems.powder_iron, 1), new ItemStack(ModItems.powder_lead, 1), new ItemStack(ModItems.nuclear_waste_tiny, 1), new ItemStack(ModItems.dust, 2)}, false, 2 });
		gasCent.put(new FluidStack(1000, Fluids.MINSOL), new Object[] { new ItemStack[] {new ItemStack(ModItems.powder_iron, 1), new ItemStack(ModItems.crystal_cleaned, 1)}, false, 2 });

	}
}
