package com.hbm.blocks.generic;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Shared extraction logic for rich ores: one drain costs exactly one unit.
 * Used by hand mining, the excavator and the mining laser.
 */
public class OreRichnessHelper {

	public static boolean isRichOre(Block b) {
		return b instanceof BlockRichOre;
	}

	public static boolean isRichOre(World world, int x, int y, int z) {
		return isRichOre(world.getBlock(x, y, z));
	}

	public static int getUnitsRemaining(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockRichOre)) return 0;
		return Math.max(0, Math.min(world.getBlockMetadata(x, y, z), BlockRichOre.MAX_UNITS - 1)) + 1;
	}

	// fortune affects output count only, never depletion rate
	public static int rollChunkCount(Random rand, int fortune) {
		fortune = Math.max(0, fortune);

		if(fortune > 0) {
			int mult = rand.nextInt(fortune + 2) - 1;
			return Math.max(mult, 0) + 1;
		}
		return 1;
	}

	// decrements richness, or turns to stone on the final unit. server only
	public static boolean consumeOneUnit(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockRichOre)) return false;
		if(world.isRemote) return false;

		int meta = Math.max(0, Math.min(world.getBlockMetadata(x, y, z), BlockRichOre.MAX_UNITS - 1));

		if(meta <= 0) {
			world.setBlock(x, y, z, Blocks.stone, 0, 3);
		} else {
			world.setBlockMetadataWithNotify(x, y, z, meta - 1, 3);
		}

		return true;
	}

	// extracts one unit. returns the chunk, or null if inapplicable
	public static ItemStack drainOneUnit(World world, int x, int y, int z, int fortune) {
		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockRichOre)) return null;

		ItemStack chunk = ((BlockRichOre) b).getChunkStack();
		chunk.stackSize = rollChunkCount(world.rand, fortune);

		if(!consumeOneUnit(world, x, y, z)) return null;

		return chunk;
	}
}
