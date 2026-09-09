package com.hbm.blocks.generic;

import java.util.Random;

import com.hbm.blocks.generic.BlockRichOre.RichOreType;
import com.hbm.config.WorldConfig;
import com.hbm.saveddata.RichOreData;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

// shared extraction logic for rich ores: one drain costs exactly one unit.
// used by hand mining, the excavator and the mining laser.
public class OreRichnessHelper {

	public static boolean isRichOre(Block b) {
		return b instanceof BlockRichOre;
	}

	public static boolean isRichOre(World world, int x, int y, int z) {
		return isRichOre(world.getBlock(x, y, z));
	}

	public static int getMaxUnits(BlockRichOre ore) {
		int max = ore.type == RichOreType.IRON ? WorldConfig.richIronUnits : WorldConfig.richCopperUnits;
		return Math.max(1, max);
	}

	// exact units left; untouched blocks read as the configured full value
	public static int getUnitsRemaining(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockRichOre)) return 0;

		BlockRichOre ore = (BlockRichOre) b;
		Integer stored = RichOreData.forWorld(world).get(x, y, z);

		if(stored != null && stored.intValue() > 0) return Math.min(stored.intValue(), getMaxUnits(ore));
		if(stored != null) RichOreData.forWorld(world).remove(x, y, z);

		return getMaxUnits(ore);
	}

	// display stage 0..7 for an exact count; identical to meta+1 while max is 8
	public static int stageFor(int units, int max) {
		if(units <= 0) return 0;
		return (int) Math.min(BlockRichOre.MAX_UNITS - 1, (units * (long) BlockRichOre.MAX_UNITS - 1) / Math.max(1, max));
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

		BlockRichOre ore = (BlockRichOre) b;
		int left = getUnitsRemaining(world, x, y, z) - 1;
		RichOreData data = RichOreData.forWorld(world);

		if(left <= 0) {
			data.remove(x, y, z);
			world.setBlock(x, y, z, Blocks.stone, 0, 3);
		} else {
			data.set(x, y, z, left);
			world.setBlockMetadataWithNotify(x, y, z, stageFor(left, getMaxUnits(ore)), 3);
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
