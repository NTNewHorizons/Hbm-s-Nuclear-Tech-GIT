package com.hbm.items.tool;

import java.util.HashMap;
import java.util.Map;

import com.hbm.main.MainRegistry;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemTooling extends ItemCraftingDegradation {

	/** NBT key used by tool crafting recipes to hand off the computed durability damage to the tool. */
	public static final String KEY_CRAFTING_DAMAGE = "craftingDurabilityDamage";

	private static final Map<Item, Integer> tierMap = new HashMap<>();

	protected ToolType type;
	protected int tier;

	public ItemTooling(ToolType type, int durability) {
		this(type, durability, 0);
	}

	public ItemTooling(ToolType type, int durability, int tier) {
		super(durability);
		this.type = type;
		this.tier = tier;
		this.setFull3D();
		this.setCreativeTab(MainRegistry.controlTab);

		if(type != null) {
			type.register(new ItemStack(this));
			tierMap.put(this, tier);
		}
	}

	public ToolType getToolType() {
		return type;
	}

	public int getTier() {
		return tier;
	}

	public static int getTier(ItemStack stack) {
		if(stack == null || stack.getItem() == null) return 0;
		return tierMap.getOrDefault(stack.getItem(), 0);
	}

	public static boolean isToolOfType(ItemStack stack, ToolType type) {
		if(stack == null || stack.getItem() == null) return false;
		for(ItemStack s : type.stacksForDisplay) {
			if(s.getItem() == stack.getItem()) return true;
		}
		return false;
	}

	public static boolean isAnyTool(ItemStack stack) {
		for(ToolType type : ToolType.values()) {
			if(isToolOfType(stack, type)) return true;
		}
		return false;
	}

	public static ItemStack getAny(ToolType type, int minTier) {
		for(ItemStack stack : type.stacksForDisplay) {
			if(getTier(stack) >= minTier) return stack.copy();
		}
		if(!type.stacksForDisplay.isEmpty()) return type.stacksForDisplay.get(0).copy();
		return null;
	}

	/**
	 * Reads the durability damage pending from a tool crafting operation.
	 * Falls back to a single point of damage for ordinary crafting.
	 */
	public static int getPendingCraftingDamage(ItemStack stack) {
		if(stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(KEY_CRAFTING_DAMAGE)) {
			return stack.getTagCompound().getInteger(KEY_CRAFTING_DAMAGE);
		}
		return 1;
	}

	@Override
	public ItemStack getContainerItem(ItemStack stack) {
		if(this.getMaxDamage() <= 0) return stack.copy();

		int damage = getPendingCraftingDamage(stack);
		ItemStack copy = stack.copy();
		copy.setItemDamage(stack.getItemDamage() + damage);
		if(copy.hasTagCompound()) {
			copy.getTagCompound().removeTag(KEY_CRAFTING_DAMAGE);
			if(copy.getTagCompound().hasNoTags()) copy.setTagCompound(null);
		}
		return copy;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fX, float fY, float fZ) {

		if(type == null) return false;

		Block b = world.getBlock(x, y, z);

		if(b instanceof IToolable) {
			if(((IToolable)b).onScrew(world, player, x, y, z, side, fX, fY, fZ, this.type)) {

				if(this.getMaxDamage() > 0)
					stack.damageItem(1, player);

				return true;
			}
		}

		return false;
	}
}
