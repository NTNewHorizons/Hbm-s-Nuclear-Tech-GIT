package com.hbm.crafting.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hbm.items.tool.ItemTooling;

import api.hbm.block.IToolable.ToolType;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class ItemToolRecipe implements IRecipe {

	private final ItemStack result;

	private final boolean isShapeless;
	private final int width;
	private final int height;
	private final Object[] shapedInputs;
	private final List<Object> shapelessInputs;
	private final ToolType[] tools;
	private final int minTier;

	private final int uniqueIngredients;
	private final int outputCount;

	private boolean mirrored;

	private ItemStack[] displayRecipe;

	public ItemToolRecipe(ItemStack result, int width, int height, Object[] shapedInputs, int minTier, ToolType... tools) {
		this.result = result;
		this.isShapeless = false;
		this.width = width;
		this.height = height;
		this.shapedInputs = shapedInputs;
		this.shapelessInputs = null;
		this.tools = tools;
		this.minTier = minTier;
		this.outputCount = result.stackSize;
		this.uniqueIngredients = countUniqueShaped();
	}

	public ItemToolRecipe setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
		return this;
	}

	public ItemToolRecipe(ItemStack result, List<Object> shapelessInputs, int minTier, ToolType... tools) {
		this.result = result;
		this.isShapeless = true;
		this.width = 0;
		this.height = 0;
		this.shapedInputs = null;
		this.shapelessInputs = shapelessInputs;
		this.tools = tools;
		this.minTier = minTier;
		this.outputCount = result.stackSize;
		this.uniqueIngredients = countUniqueShapeless();
	}

	private int countUniqueShaped() {
		List<Object> unique = new ArrayList<>();
		for(int i = 0; i < 9; i++) {
			Object ing = i < shapedInputs.length ? shapedInputs[i] : null;
			if(ing != null && !containsIngredient(unique, ing)) {
				unique.add(ing);
			}
		}
		return unique.size();
	}

	private int countUniqueShapeless() {
		List<Object> unique = new ArrayList<>();
		for(Object ing : shapelessInputs) {
			if(!containsIngredient(unique, ing)) {
				unique.add(ing);
			}
		}
		return unique.size();
	}

	private boolean containsIngredient(List<Object> list, Object ing) {
		for(Object existing : list) {
			if(ingredientsMatch(existing, ing)) return true;
		}
		return false;
	}

	private boolean ingredientsMatch(Object a, Object b) {
		if(a instanceof ItemStack && b instanceof ItemStack) {
			ItemStack sa = (ItemStack) a;
			ItemStack sb = (ItemStack) b;
			return sa.getItem() == sb.getItem() && (sa.getItemDamage() == sb.getItemDamage() || sa.getItemDamage() == OreDictionary.WILDCARD_VALUE || sb.getItemDamage() == OreDictionary.WILDCARD_VALUE);
		}
		if(a instanceof Item && b instanceof Item) {
			return a == b;
		}
		if(a instanceof String && b instanceof String) {
			return a.equals(b);
		}
		return false;
	}

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		if(isShapeless) {
			return matchesShapeless(inv);
		} else {
			return matchesShaped(inv);
		}
	}

	private void writeToolDamageNBT(InventoryCrafting inv) {
		int damage = calcDurabilityDamage();
		for(int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if(slot != null && ItemTooling.isAnyTool(slot)) {
				if(!slot.hasTagCompound()) {
					slot.setTagCompound(new NBTTagCompound());
				}
				slot.getTagCompound().setInteger(ItemTooling.KEY_CRAFTING_DAMAGE, damage);
			}
		}
	}

	private boolean matchesShaped(InventoryCrafting inv) {
		if(checkShapedPattern(inv, false)) return true;
		return mirrored && checkShapedPattern(inv, true);
	}

	private boolean checkShapedPattern(InventoryCrafting inv, boolean mirror) {
		boolean[] toolFound = new boolean[tools.length];
		int gridSize = inv.getSizeInventory();

		for(int i = 0; i < gridSize; i++) {
			ItemStack slot = inv.getStackInSlot(i);
			Object required = getShapedInput(i, mirror);

			if(required != null) {
				if(slot == null) return false;
				if(!itemMatches(required, slot)) return false;

			} else {
				if(slot != null) {
					boolean isTool = false;
					for(int t = 0; t < tools.length; t++) {
						if(!toolFound[t] && ItemTooling.isToolOfType(slot, tools[t]) && ItemTooling.getTier(slot) >= minTier) {
							toolFound[t] = true;
							isTool = true;
							break;
						}
					}
					if(!isTool) return false;
				}
			}
		}

		if(gridSize < shapedInputs.length) {
			for(int i = gridSize; i < shapedInputs.length; i++) {
				if(getShapedInput(i, mirror) != null) return false;
			}
		}

		for(boolean found : toolFound) {
			if(!found) return false;
		}

		return true;
	}

	private Object getShapedInput(int gridIndex, boolean mirror) {
		if(!mirror) return gridIndex < shapedInputs.length ? shapedInputs[gridIndex] : null;
		int r = gridIndex / 3;
		int c = gridIndex % 3;
		int mirroredCol = width - 1 - c;
		if(mirroredCol < 0 || mirroredCol >= width) return null;
		int mirroredIndex = r * 3 + mirroredCol;
		return mirroredIndex < shapedInputs.length ? shapedInputs[mirroredIndex] : null;
	}

	private boolean matchesShapeless(InventoryCrafting inv) {
		List<ItemStack> gridItems = new ArrayList<>();
		for(int i = 0; i < 9; i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if(slot != null) gridItems.add(slot);
		}

		List<ItemStack> remaining = new ArrayList<>(gridItems);
		for(Object input : shapelessInputs) {
			boolean found = false;
			Iterator<ItemStack> it = remaining.iterator();
			while(it.hasNext()) {
				ItemStack stack = it.next();
				if(itemMatches(input, stack)) {
					it.remove();
					found = true;
					break;
				}
			}
			if(!found) return false;
		}

		boolean[] toolFound = new boolean[tools.length];
		for(ItemStack stack : remaining) {
			boolean isTool = false;
			for(int t = 0; t < tools.length; t++) {
				if(!toolFound[t] && ItemTooling.isToolOfType(stack, tools[t]) && ItemTooling.getTier(stack) >= minTier) {
					toolFound[t] = true;
					isTool = true;
					break;
				}
			}
			if(!isTool) return false;
		}

		for(boolean found : toolFound) {
			if(!found) return false;
		}

		return true;
	}

	private boolean itemMatches(Object required, ItemStack slot) {
		if(required instanceof ItemStack) {
			ItemStack req = (ItemStack) required;
			return slot.getItem() == req.getItem() &&
					(req.getItemDamage() == OreDictionary.WILDCARD_VALUE || slot.getItemDamage() == req.getItemDamage());
		} else if(required instanceof Block) {
			return slot.getItem() == Item.getItemFromBlock((Block) required);
		} else if(required instanceof Item) {
			return slot.getItem() == required;
		} else if(required instanceof String) {
			List<ItemStack> ores = OreDictionary.getOres((String) required);
			for(ItemStack ore : ores) {
				if(OreDictionary.itemMatches(ore, slot, false)) return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		writeToolDamageNBT(inv);
		return result.copy();
	}

	private int calcDurabilityDamage() {
		return Math.max(1, uniqueIngredients + outputCount);
	}

	@Override
	public int getRecipeSize() {
		if(isShapeless) {
			return shapelessInputs.size() + tools.length;
		}
		int count = 0;
		for(Object o : shapedInputs) {
			if(o != null) {
				count++;
			}
		}
		count += tools.length;
		return Math.min(count, 9);
	}

	@Override
	public ItemStack getRecipeOutput() {
		return result;
	}

	public ItemStack[] getDisplayRecipe() {
		if(displayRecipe == null) buildDisplayRecipe();
		return displayRecipe;
	}

	private void buildDisplayRecipe() {
		displayRecipe = new ItemStack[9];

		if(isShapeless) {
			int slot = 0;
			if(shapelessInputs != null) {
				for(Object input : shapelessInputs) {
					if(slot >= 9) break;
					displayRecipe[slot++] = getItemStack(input);
				}
			}
			for(ToolType tool : tools) {
				if(slot >= 9) break;
				displayRecipe[slot++] = ItemTooling.getAny(tool, minTier);
			}
		} else {
			if(shapedInputs != null) {
				for(int i = 0; i < 9; i++) {
					if(i < shapedInputs.length) {
						displayRecipe[i] = getItemStack(shapedInputs[i]);
					}
				}
			}
			int toolSlot = 8;
			for(ToolType tool : tools) {
				while(toolSlot >= 0 && displayRecipe[toolSlot] != null) {
					toolSlot--;
				}
				if(toolSlot >= 0) {
					displayRecipe[toolSlot] = ItemTooling.getAny(tool, minTier);
					toolSlot--;
				}
			}
		}
	}

	private ItemStack getItemStack(Object obj) {
		if(obj instanceof ItemStack) return ((ItemStack) obj).copy();
		if(obj instanceof Block) return new ItemStack((Block) obj);
		if(obj instanceof Item) return new ItemStack((Item) obj);
		if(obj instanceof String) {
			List<ItemStack> ores = OreDictionary.getOres((String) obj);
			if(!ores.isEmpty()) return ores.get(0).copy();
		}
		return null;
	}

	public ToolType[] getRequiredTools() {
		return tools;
	}

	public boolean isShapeless() {
		return isShapeless;
	}
}
