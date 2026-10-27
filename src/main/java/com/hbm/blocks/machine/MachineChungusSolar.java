package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.machine.TileEntityChungusSolar;
import com.hbm.util.BobMathUtil;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.block.IToolable;
import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineChungusSolar extends BlockDummyable implements ITooltipProvider, ILookOverlay, IToolable {

	public MachineChungusSolar(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityChungusSolar();
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] { 0, 0, 16, 0, 1, 1 };
	}

	@Override
	public int getOffset() {
		return 0;
	}

	private List<BlockPos> panelBlocks(int x, int y, int z, ForgeDirection facing, int rotation) {
		List<BlockPos> blocks = new ArrayList<>();
		// Local model X points toward the port; local Z spans the panel's width.
		int acrossX = -facing.offsetZ;
		int acrossZ = facing.offsetX;
		int[] horizontal = { 1, 1, 0, -1 };
		int[] vertical = { 0, -1, -1, -1 };
		int shape = rotation & 3;
		for(int length = 1; length <= 16; length++) {
			for(int width = -1; width <= 1; width++) {
				blocks.add(new BlockPos(x - facing.offsetX * length + acrossX * width * horizontal[shape],
						y + width * vertical[shape], z - facing.offsetZ * length + acrossZ * width * horizontal[shape]));
			}
		}
		return blocks;
	}

	public List<BlockPos> getSolarCellBlocks(TileEntityChungusSolar solar) {
		List<BlockPos> blocks = panelBlocks(solar.xCoord, solar.yCoord, solar.zCoord,
				ForgeDirection.getOrientation(solar.getBlockMetadata() - offset), solar.panelRotation);
		return blocks.subList(3, blocks.size());
	}

	@Override
	protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
		if(!world.getBlock(x, y, z).isReplaceable(world, x, y, z)) return false;
		for(BlockPos pos : panelBlocks(x, y, z, dir, 0)) {
			if(!world.getBlock(pos.getX(), pos.getY(), pos.getZ()).isReplaceable(world, pos.getX(), pos.getY(), pos.getZ())) return false;
		}
		return true;
	}

	public boolean updateLegacyFootprint(TileEntityChungusSolar solar) {
		World world = solar.getWorldObj();
		int x = solar.xCoord, y = solar.yCoord, z = solar.zCoord;
		ForgeDirection facing = ForgeDirection.getOrientation(solar.getBlockMetadata() - offset);
		List<BlockPos> legacy = new ArrayList<>();
		for(int length = 0; length <= 17; length++) {
			for(int width = -1; width <= 1; width++) {
				int bx = x - facing.offsetX * length - facing.offsetZ * width;
				int bz = z - facing.offsetZ * length + facing.offsetX * width;
				if(!world.blockExists(bx, y, bz)) return false;
				if(world.getBlock(bx, y, bz) != this || world.getBlockMetadata(bx, y, bz) >= extra) continue;
				int[] core = findCore(world, bx, y, bz);
				if(core != null && core[0] == x && core[1] == y && core[2] == z) legacy.add(new BlockPos(bx, y, bz));
			}
		}
		if(legacy.isEmpty()) return true;
		List<BlockPos> target = panelBlocks(x, y, z, facing, solar.panelRotation);
		boolean blocked = false;
		for(BlockPos pos : target) {
			if(pos.getY() < 0 || pos.getY() >= world.getHeight()) {
				blocked = true;
				break;
			}
			if(!world.blockExists(pos.getX(), pos.getY(), pos.getZ())) return false;
			if(!legacy.contains(pos) && !world.getBlock(pos.getX(), pos.getY(), pos.getZ()).isReplaceable(world, pos.getX(), pos.getY(), pos.getZ())) {
				blocked = true;
				break;
			}
		}
		if(blocked) {
			// Old rotated arrays still occupied a flat rectangle. Keep them flat if the new pose is blocked.
			target = panelBlocks(x, y, z, facing, 0);
			for(BlockPos pos : target) {
				if(!legacy.contains(pos) && !world.getBlock(pos.getX(), pos.getY(), pos.getZ()).isReplaceable(world, pos.getX(), pos.getY(), pos.getZ())) return false;
			}
			solar.panelRotation = 0;
		}
		boolean previous = safeRem;
		safeRem = true;
		try {
			for(BlockPos pos : legacy) world.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
			for(BlockPos pos : target) world.setBlock(pos.getX(), pos.getY(), pos.getZ(), this, facing.getOpposite().ordinal() + extra, 2);
		} finally {
			safeRem = previous;
		}
		solar.markChanged();
		return true;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		boolean previous = safeRem;
		safeRem = true;
		try {
			for(BlockPos pos : panelBlocks(x, y, z, dir, 0)) {
				world.setBlock(pos.getX(), pos.getY(), pos.getZ(), this, dir.getOpposite().ordinal() + extra, 2);
			}
		} finally {
			safeRem = previous;
		}
	}

	@Override
	public int[] findCoreRec(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if(meta < extra || meta >= 12) return super.findCoreRec(world, x, y, z);
		return findPanelCore(world, x, y, z, meta);
	}

	private int[] findPanelCore(IBlockAccess world, int x, int y, int z, int meta) {
		ForgeDirection facing = ForgeDirection.getOrientation(meta - extra).getOpposite();
		for(int length = 1; length <= 16; length++) {
			for(int dy = -1; dy <= 1; dy++) {
				for(int width = -1; width <= 1; width++) {
					int cx = x + facing.offsetX * length - facing.offsetZ * width;
					int cy = y + dy;
					int cz = z + facing.offsetZ * length + facing.offsetX * width;
					if(world.getBlock(cx, cy, cz) != this || world.getBlockMetadata(cx, cy, cz) != facing.ordinal() + offset) continue;
					TileEntity te = world.getTileEntity(cx, cy, cz);
					if(te instanceof TileEntityChungusSolar && panelBlocks(cx, cy, cz, facing,
							((TileEntityChungusSolar) te).panelRotation).contains(new BlockPos(x, y, z))) {
						return new int[] { cx, cy, cz };
					}
				}
			}
		}
		return null;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		if(safeRem || world.isRemote) return;
		if(world.getBlockMetadata(x, y, z) >= extra && world.getBlockMetadata(x, y, z) < 12) {
			// Diagonal panel blocks are not face-connected to their core.
			if(world.checkChunksExist(x - 18, y - 1, z - 18, x + 18, y + 1, z + 18) && findCore(world, x, y, z) == null) {
				world.setBlockToAir(x, y, z);
			}
			return;
		}
		super.onNeighborBlockChange(world, x, y, z, block);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if(!safeRem && !world.isRemote) {
			// The removed block may already be air when this callback runs, so use its supplied metadata.
			int[] core = meta >= 12 ? new int[] { x, y, z }
					: meta >= extra ? findPanelCore(world, x, y, z, meta) : super.findCoreRec(world, x, y, z);
			if(core != null) {
				ForgeDirection facing = ForgeDirection.getOrientation((meta >= 12 ? meta : world.getBlockMetadata(core[0], core[1], core[2])) - offset);
				TileEntity te = world.getTileEntity(core[0], core[1], core[2]);
				if(te instanceof TileEntityChungusSolar) {
					boolean previous = safeRem;
					safeRem = true;
					try {
						for(BlockPos pos : panelBlocks(core[0], core[1], core[2], facing, ((TileEntityChungusSolar) te).panelRotation)) {
							if(world.getBlock(pos.getX(), pos.getY(), pos.getZ()) == this) world.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
						}
						if(meta < 12) world.setBlockToAir(core[0], core[1], core[2]);
					} finally {
						safeRem = previous;
					}
				}
			}
		}
		// Removal above already handles the entire array, including diagonal blocks.
		boolean previous = safeRem;
		safeRem = true;
		try {
			super.breakBlock(world, x, y, z, block, meta);
		} finally {
			safeRem = previous;
		}
	}

	@Override
	public int[][] getAllPorts(ForgeDirection dir) {
		return rotatePorts(new int[][] {
			{ 0, 0, 0, ForgeDirection.SOUTH.ordinal() },
		}, dir);
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;
		if(world.isRemote) return true;

		int[] pos = findCore(world, x, y, z);
		if(pos == null) return false;
		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityChungusSolar)) return false;

		TileEntityChungusSolar solar = (TileEntityChungusSolar) te;
		ForgeDirection facing = ForgeDirection.getOrientation(solar.getBlockMetadata() - offset);
		int nextRotation = (solar.panelRotation + 1) % 8;
		List<BlockPos> oldBlocks = panelBlocks(pos[0], pos[1], pos[2], facing, solar.panelRotation);
		List<BlockPos> newBlocks = panelBlocks(pos[0], pos[1], pos[2], facing, nextRotation);
		for(BlockPos target : newBlocks) {
			int tx = target.getX(), ty = target.getY(), tz = target.getZ();
			if(ty < 0 || ty >= world.getHeight() || !world.blockExists(tx, ty, tz)) return false;
			if(oldBlocks.contains(target) && world.getBlock(tx, ty, tz) == this) continue;
			if(!world.getBlock(tx, ty, tz).isReplaceable(world, tx, ty, tz)) return false;
		}
		for(BlockPos old : oldBlocks) {
			if(!world.blockExists(old.getX(), old.getY(), old.getZ())) return false;
		}
		boolean previous = safeRem;
		safeRem = true;
		try {
			for(BlockPos old : oldBlocks) {
				if(!newBlocks.contains(old) && world.getBlock(old.getX(), old.getY(), old.getZ()) == this) {
					world.setBlockToAir(old.getX(), old.getY(), old.getZ());
				}
			}
			solar.panelRotation = nextRotation;
			for(BlockPos target : newBlocks) {
				world.setBlock(target.getX(), target.getY(), target.getZ(), this, facing.getOpposite().ordinal() + extra, 2);
			}
		} finally {
			safeRem = previous;
		}
		solar.markChanged();
		solar.networkPackNT(50);
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "hbm:item.upgradePlug", 1.0F, 1.0F);
		return true;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		this.addStandardInfo(stack, player, list, ext);
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = findCore(world, x, y, z);
		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityChungusSolar)) return;

		TileEntityChungusSolar solar = (TileEntityChungusSolar) te;

		List<String> text = new ArrayList<>();
		text.add(EnumChatFormatting.GREEN + "-> " + EnumChatFormatting.RESET + BobMathUtil.getShortNumber(solar.getPower()) + " / " + BobMathUtil.getShortNumber(solar.getMaxPower()) + "HE");
		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff8000, 0x404000, text);
	}
}
