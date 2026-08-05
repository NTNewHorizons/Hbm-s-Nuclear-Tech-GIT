package com.hbm.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemCircuit.EnumCircuitType;
import com.hbm.lib.RefStrings;
import com.hbm.main.CraftingManager;
import com.hbm.main.MainRegistry;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CustomMachineConfigJSON {

	public static final Gson gson = new Gson();
	public static final org.apache.logging.log4j.Logger logger = MainRegistry.logger;
	public static HashMap<String, MachineConfiguration> customMachines = new HashMap();
	public static List<MachineConfiguration> niceList = new ArrayList();

	public static void initialize() {
		File folder = MainRegistry.configHbmDir;

		File config = new File(folder.getAbsolutePath() + File.separatorChar + "hbmCustomMachines.json");

		if(!config.exists()) {
			writeDefault(config);
		}
		readConfig(config);
	}

	public static void writeDefault(File config) {

		try {
			JsonWriter writer = new JsonWriter(new FileWriter(config));
			writer.setIndent("  ");
			writer.beginObject();
				writer.name("machines").beginArray();

					writer.beginObject();
						writer.name("recipeKey").value("paperPress");
						writer.name("unlocalizedName").value("paperPress");
						writer.name("localization").beginObject();
							writer.name("de_DE").value("Papierpresse");
						writer.endObject();
						writer.name("localizedName").value("Paper Press");
						writer.name("fluidInCount").value(1);
						writer.name("fluidInCap").value(1_000);
						writer.name("itemInCount").value(1);
						writer.name("fluidOutCount").value(0);
						writer.name("fluidOutCap").value(0);
						writer.name("itemOutCount").value(1);
						writer.name("generatorMode").value(false);
						writer.name("maxPollutionCap").value(100);
						writer.name("fluxMode").value(false);
						writer.name("recipeSpeedMult").value(1.0D);
						writer.name("recipeConsumptionMult").value(1.0D);
						writer.name("maxPower").value(10_000L);
						writer.name("maxHeat").value(0);
						writer.name("progressSound").value("hbm:block.assemblerOperate");
						writer.name("materialInCount").value(0);
						writer.name("materialInCap").value(0);
						writer.name("materialOut").value(false);

						writer.name("recipeShape").beginArray();
							writer.value("IPI").value("PCP").value("IPI");
						writer.endArray();

						writer.name("recipeParts").beginArray().setIndent("");
							writer.value("I");
							SerializableRecipe.writeAStack(new OreDictStack(OreDictManager.STEEL.ingot()), writer);
							writer.setIndent("");
							writer.value("P");
							SerializableRecipe.writeAStack(new OreDictStack(OreDictManager.STEEL.plate()), writer);
							writer.setIndent("");
							writer.value("C");
							SerializableRecipe.writeAStack(new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC), writer);
						writer.endArray().setIndent("  ");

						writer.name("components").beginArray();

							for(int x = -1; x <= 1; x++) {
								for(int y = -1; y <= 1; y++) {
									for(int z = 0; z <= 2; z++) {
										if(!(x == 0 && y == 0 && z == 1) && !(x == 0 && z == 0)) {
											writer.beginObject().setIndent("");
												writer.name("block").value(y == 0 ? "hbm:tile.cm_sheet" : "hbm:tile.cm_block");
												writer.name("x").value(x);
												writer.name("y").value(y);
												writer.name("z").value(z);
												writer.name("metas").beginArray();
													writer.value(0);
												writer.endArray();
											writer.endObject().setIndent("  ");
										}
									}
								}
							}

							writer.beginObject().setIndent("");
								writer.name("block").value("hbm:tile.cm_port");
								writer.name("x").value(0);
								writer.name("y").value(-1);
								writer.name("z").value(0);
								writer.name("metas").beginArray();
									writer.value(0);
								writer.endArray();
							writer.endObject().setIndent("  ");

							writer.beginObject().setIndent("");
								writer.name("block").value("hbm:tile.cm_port");
								writer.name("x").value(0);
								writer.name("y").value(1);
								writer.name("z").value(0);
								writer.name("metas").beginArray();
									writer.value(0);
								writer.endArray();
							writer.endObject().setIndent("  ");

						writer.endArray();

						writer.name("customModel").beginObject();
							writer.name("model").value("models/machines/furnace_steel.obj");
							writer.name("modelTexture").value("textures/models/machines/furnace_steel.png");
							writer.name("model_x").value(0.0);
							writer.name("model_y").value(2.0);
							writer.name("model_z").value(1.0);
								// Bounding box auto-calculated; use modelRenderPadding to adjust
								writer.name("modelRenderPadding").beginObject();
									writer.name("x").value(1.0);
									writer.name("y").value(2.0);
									writer.name("z").value(1.0);
								writer.endObject();
						writer.endObject();

					writer.endObject();

				writer.endArray();
			writer.endObject();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void readConfig(File config) {

		try {
			JsonObject json = gson.fromJson(new InputStreamReader(Files.newInputStream(config.toPath()), StandardCharsets.UTF_8), JsonObject.class);
			JsonArray machines = json.get("machines").getAsJsonArray();

			for(int i = 0; i < machines.size(); i++) {
				JsonObject machineObject = machines.get(i).getAsJsonObject();

				MachineConfiguration configuration = new MachineConfiguration();
				configuration.recipeKey = machineObject.get("recipeKey").getAsString();
				configuration.unlocalizedName = machineObject.get("unlocalizedName").getAsString();
				configuration.localizedName = machineObject.get("localizedName").getAsString();
				if(machineObject.has("localization")) {
					JsonObject localization = machineObject.get("localization").getAsJsonObject();
					for(Entry<String, JsonElement> entry : localization.entrySet()) {
						configuration.localization.put(entry.getKey(), entry.getValue().getAsString());
					}
				}
				configuration.fluidInCount = machineObject.get("fluidInCount").getAsInt();
				configuration.fluidInCap = machineObject.get("fluidInCap").getAsInt();
				configuration.itemInCount = machineObject.get("itemInCount").getAsInt();
				configuration.fluidOutCount = machineObject.get("fluidOutCount").getAsInt();
				configuration.fluidOutCap = machineObject.get("fluidOutCap").getAsInt();
				configuration.itemOutCount = machineObject.get("itemOutCount").getAsInt();
				configuration.generatorMode = machineObject.get("generatorMode").getAsBoolean();
				if(machineObject.has("maxPollutionCap")) configuration.maxPollutionCap = machineObject.get("maxPollutionCap").getAsInt();
				if(machineObject.has("fluxMode")) configuration.fluxMode = machineObject.get("fluxMode").getAsBoolean();
				configuration.recipeSpeedMult = machineObject.get("recipeSpeedMult").getAsDouble();
				configuration.recipeConsumptionMult = machineObject.get("recipeConsumptionMult").getAsDouble();
				configuration.maxPower = machineObject.get("maxPower").getAsLong();
				if(machineObject.has("maxHeat")) configuration.maxHeat = machineObject.get("maxHeat").getAsInt();
				if(machineObject.has("progressSound")) configuration.progressSound = machineObject.get("progressSound").getAsString();
				if(machineObject.has("materialInCount")) configuration.materialInCount = machineObject.get("materialInCount").getAsInt();
				if(machineObject.has("materialInCap")) configuration.materialInCap = machineObject.get("materialInCap").getAsInt();
				if(machineObject.has("materialOut")) configuration.materialOut = machineObject.get("materialOut").getAsBoolean();

				if(machineObject.has("recipeShape") && machineObject.has("recipeParts")) {
					try {
						JsonArray recipeShape = machineObject.get("recipeShape").getAsJsonArray();
						JsonArray recipeParts = machineObject.get("recipeParts").getAsJsonArray();

						Object[] parts = new Object[recipeShape.size() + recipeParts.size()];

						for(int j = 0; j < recipeShape.size(); j++) {
							parts[j] = recipeShape.get(j).getAsString();
						}

						for(int j = 0; j < recipeParts.size(); j++) {
							Object o = null;

							if(j % 2 == 0) {
								o = recipeParts.get(j).getAsString().charAt(0); //god is dead and we killed him
							} else {
								AStack a = SerializableRecipe.readAStack(recipeParts.get(j).getAsJsonArray());

								if(a instanceof ComparableStack) o = ((ComparableStack) a).toStack();
								if(a instanceof OreDictStack) o = ((OreDictStack) a).name;
							}

							parts[j + recipeShape.size()] = o;
						}

						ItemStack stack = new ItemStack(ModBlocks.custom_machine, 1, i + 100);
						stack.stackTagCompound = new NBTTagCompound();
						stack.stackTagCompound.setString("machineType", configuration.unlocalizedName);

						CraftingManager.addRecipeAuto(stack, parts);
					} catch(Exception ex) {
						MainRegistry.logger.error("Caught exception trying to parse core recipe for custom machine " + configuration.unlocalizedName);
						MainRegistry.logger.error("recipeShape was" + machineObject.get("recipeShape").toString());
						MainRegistry.logger.error("recipeParts was" + machineObject.get("recipeParts").toString());
					}
				}

				JsonArray components = machineObject.get("components").getAsJsonArray();
				configuration.components = new ArrayList();
				// Bounding box auto-calculation: track min/max of all component positions
				double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
				double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
				for(int j = 0; j < components.size(); j++) {
					JsonObject compObject = components.get(j).getAsJsonObject();
					ComponentDefinition compDef = new ComponentDefinition();
					compDef.block = (Block) Block.blockRegistry.getObject(compObject.get("block").getAsString());
					compDef.x = compObject.get("x").getAsInt();
					compDef.y = compObject.get("y").getAsInt();
					compDef.z = compObject.get("z").getAsInt();
					compDef.allowedMetas = new HashSet();
					compDef.metaList = new ArrayList();
					compDef.metas = compObject.get("metas").getAsJsonArray();
					for(int k = 0; k < compDef.metas.size(); k++) {
						int metaVal = compDef.metas.get(k).getAsInt();
						compDef.allowedMetas.add(metaVal);
						compDef.metaList.add(metaVal);
					}

					configuration.components.add(compDef);
					if(machineObject.has("customModel")) {
						minX = Math.min(minX, compDef.x);
						minY = Math.min(minY, compDef.y);
						minZ = Math.min(minZ, compDef.z);
						maxX = Math.max(maxX, compDef.x);
						maxY = Math.max(maxY, compDef.y);
						maxZ = Math.max(maxZ, compDef.z);
					}
				}
				// Safeguard: if no components had customModel, set sane defaults
				if(minX == Double.POSITIVE_INFINITY) { minX = -1; minY = 0; minZ = 0; maxX = 1; maxY = 2; maxZ = 1; }
				configuration.customModel = null;
				if(machineObject.has("customModel")) {
					JsonObject modelObject = machineObject.get("customModel").getAsJsonObject();
					configuration.customModel = new MachineConfiguration.CustomModel();
					configuration.customModel.customModel = modelObject.get("model").getAsString();
					configuration.customModel.modelTexture = modelObject.get("modelTexture").getAsString();
					configuration.customModel.model_x = modelObject.get("model_x").getAsDouble();
					configuration.customModel.model_y = modelObject.get("model_y").getAsDouble();
					configuration.customModel.model_z = modelObject.get("model_z").getAsDouble();

				// Read optional modelRenderPadding (simpler alternative to manual bounding box)
					double padX = 1.0, padY = 2.0, padZ = 1.0;
				if(modelObject.has("modelRenderPadding")) {
						JsonObject pad = modelObject.get("modelRenderPadding").getAsJsonObject();
						if(pad.has("x")) padX = pad.get("x").getAsDouble();
						if(pad.has("y")) padY = pad.get("y").getAsDouble();
						if(pad.has("z")) padZ = pad.get("z").getAsDouble();
					}

					// Auto-calculate bounding box from components + padding
					// Backward compat: if old fields specified, they override auto-calculation
					double bbX1 = minX - padX;
					double bbY1 = minY - padY;
					double bbZ1 = minZ - padZ;
					double bbX2 = maxX + padX;
					double bbY2 = maxY + padY;
					double bbZ2 = maxZ + padZ;
					if(modelObject.has("model_Bounding_x1")) bbX1 = modelObject.get("model_Bounding_x1").getAsDouble();
					if(modelObject.has("model_Bounding_y1")) bbY1 = modelObject.get("model_Bounding_y1").getAsDouble();
					if(modelObject.has("model_Bounding_z1")) bbZ1 = modelObject.get("model_Bounding_z1").getAsDouble();
					if(modelObject.has("model_Bounding_x2")) bbX2 = modelObject.get("model_Bounding_x2").getAsDouble();
					if(modelObject.has("model_Bounding_y2")) bbY2 = modelObject.get("model_Bounding_y2").getAsDouble();
					if(modelObject.has("model_Bounding_z2")) bbZ2 = modelObject.get("model_Bounding_z2").getAsDouble();
					configuration.customModel.model_Bounding_x1 = bbX1;
					configuration.customModel.model_Bounding_y1 = bbY1;
					configuration.customModel.model_Bounding_z1 = bbZ1;
					configuration.customModel.model_Bounding_x2 = bbX2;
					configuration.customModel.model_Bounding_y2 = bbY2;
					configuration.customModel.model_Bounding_z2 = bbZ2;
				}

				// Validate parsed configuration
				MachineConfiguration.ValidationResult vr = configuration.validate();
				if(!vr.valid) {
					logger.error("Custom machine '" + configuration.unlocalizedName + "' has validation errors:");
					for(String err : vr.errors) logger.error("  ERR: " + err);
					continue;
				}
				if(!vr.warnings.isEmpty()) {
					for(String warn : vr.warnings)
						logger.warn("Custom machine '" + configuration.unlocalizedName + "': " + warn);
				}

				if(!(customMachines.size()>0 && customMachines.containsKey(machineObject.get("unlocalizedName").getAsString()))){
					customMachines.put(configuration.unlocalizedName, configuration);
					niceList.add(configuration);
				}

			}

		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	public static class MachineConfiguration {

		/** The name of the recipe set that this machine can handle */
		public String recipeKey;
		/** The internal name of this machine */
		public String unlocalizedName;
		/** The display name of this machine */
		public String localizedName;
		public HashMap<String, String> localization = new HashMap();;

		public int fluidInCount;
		public int fluidInCap;
		public int itemInCount;
		public int fluidOutCount;
		public int fluidOutCap;
		public int itemOutCount;
		/** Whether inputs should be used up when the process begins */
		public boolean generatorMode;
		public int maxPollutionCap;
		public boolean fluxMode;
		public double recipeSpeedMult = 1D;
		public double recipeConsumptionMult = 1D;
		public long maxPower;
		public int maxHeat;
		public String progressSound;
		public int materialInCount;
		public int materialInCap;
		public boolean materialOut;


		/** Definitions of blocks that this machine is composed of */
		public List<ComponentDefinition> components;

		public static class ComponentDefinition {
			public Block block;
			public Set<Integer> allowedMetas;
			public List<Integer> metaList;
			public JsonArray metas;
			public int x;
			public int y;
			public int z;
		}
		public CustomModel customModel;
		public static class CustomModel {
			public String customModel;
			public String modelTexture;
			public double model_x;
			public double model_y;
			public double model_z;
			public double model_Bounding_x1;
			public double model_Bounding_y1;
			public double model_Bounding_z1;
			public double model_Bounding_x2;
			public double model_Bounding_y2;
			public double model_Bounding_z2;

		}

		/** Validates this machine configuration and returns any errors/warnings found */
		public ValidationResult validate() {
			List<String> errors = new ArrayList<String>();
			List<String> warnings = new ArrayList<String>();

			if(recipeKey == null || recipeKey.isEmpty()) errors.add("recipeKey is required");
			if(unlocalizedName == null || unlocalizedName.isEmpty()) errors.add("unlocalizedName is required");
			if(maxPower <= 0) errors.add("maxPower must be > 0, got " + maxPower);

			if(components == null || components.isEmpty()) {
				errors.add("At least one component is required");
			} else {
				for(int i = 0; i < components.size(); i++) {
					ComponentDefinition c = components.get(i);
					if(c.block == null) errors.add("Component [" + i + "] has null block");
					if(c.allowedMetas == null || c.allowedMetas.isEmpty())
						warnings.add("Component [" + i + "] has no allowed metas");
				}
			}

			if(customModel != null) {
				if(customModel.customModel == null || customModel.customModel.isEmpty())
					errors.add("customModel.model is required when customModel section is present");
				if(customModel.modelTexture == null || customModel.modelTexture.isEmpty())
					errors.add("customModel.modelTexture is required when customModel section is present");
			}

			return new ValidationResult(errors.isEmpty(), errors, warnings);
		}

		public static class ValidationResult {
			public final boolean valid;
			public final List<String> errors;
			public final List<String> warnings;

			public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
				this.valid = valid;
				this.errors = errors;
				this.warnings = warnings;
			}
		}
	}
}
