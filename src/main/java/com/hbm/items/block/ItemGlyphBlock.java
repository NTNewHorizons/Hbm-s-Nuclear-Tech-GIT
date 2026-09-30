package com.hbm.items.block;

import com.hbm.util.i18n.I18nUtil;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemGlyphBlock extends ItemBlock {

	public ItemGlyphBlock(Block block) {
		super(block);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
	}

	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean bool) {

		switch(itemstack.getItemDamage()) {
		case 0: list.add(I18nUtil.resolveKey("desc.item.glyph_block.1")); break;
		case 1: list.add(I18nUtil.resolveKey("desc.item.glyph_block.2")); break;
		case 2: list.add(I18nUtil.resolveKey("desc.item.glyph_block.3")); break;
		case 3: list.add(I18nUtil.resolveKey("desc.item.glyph_block.4")); break;
		case 4: list.add(I18nUtil.resolveKey("desc.item.glyph_block.5")); break;
		case 5: list.add(I18nUtil.resolveKey("desc.item.glyph_block.6")); break;
		case 6: list.add(I18nUtil.resolveKey("desc.item.glyph_block.7")); break;
		case 7: list.add(I18nUtil.resolveKey("desc.item.glyph_block.8")); break;
		case 8: list.add(I18nUtil.resolveKey("desc.item.glyph_block.9")); break;
		case 9: list.add(I18nUtil.resolveKey("desc.item.glyph_block.10")); break;
		case 10: list.add(I18nUtil.resolveKey("desc.item.glyph_block.11")); break;
		case 11: list.add(I18nUtil.resolveKey("desc.item.glyph_block.12")); break;
		case 12: list.add(I18nUtil.resolveKey("desc.item.glyph_block.13")); break;
		case 13: list.add("13"); break;
		case 14: list.add(I18nUtil.resolveKey("desc.item.glyph_block.14")); break;
		case 15: list.add(I18nUtil.resolveKey("desc.item.glyph_block.15")); break;
		}
	}

    public int getMetadata(int meta)
    {
        return meta;
    }
}
