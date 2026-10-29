package com.hbm.asm;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class HbmLateMixinLoader implements ILateMixinLoader {

	@Override
	public String getMixinConfig() {
		return "mixins.hbm.late.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedMods) {
		return loadedMods.contains("StorageDrawers") ? Collections.singletonList("MixinTileEntityDrawers") : Collections.emptyList();
	}
}
