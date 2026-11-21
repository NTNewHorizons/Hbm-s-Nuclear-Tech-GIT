package com.hbm.mixin.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;

import api.hbm.redstoneoverradio.IRORValueProvider;
import net.minecraft.item.ItemStack;

@Mixin(value = TileEntityDrawers.class, remap = false)
public abstract class MixinTileEntityDrawers implements IRORValueProvider {

	@Shadow public abstract int getDrawerCount();
	@Shadow public abstract IDrawer getDrawer(int slot);

	@Override
	public String[] getFunctionInfo() {
		return new String[] { PREFIX_VALUE + "type" };
	}

	@Override
	public String provideRORValue(String name) {
		if(!(PREFIX_VALUE + "type").equals(name)) return null;
		IDrawer drawer = getDrawerCount() > 0 ? getDrawer(0) : null;
		ItemStack stack = drawer == null ? null : drawer.getStoredItemPrototype();
		return stack == null ? "None" : stack.getDisplayName();
	}
}
