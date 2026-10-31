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

	public static final int PANEL_LENGTH = 16;
	public static final int PANEL_WIDTH = 3;
	public static final int PANEL_ROTATIONS = 8;
	// Rounded (across, up) offsets for an outer row at 0, 45, 90 and 135 degrees.
	// Multiplying by width -1, 0 or 1 mirrors that row around the panel's axis.
	private static final int[] HORIZONTAL_OFFSETS = { 1, 1, 0, -1 };
	private static final int[] VERTICAL_OFFSETS = { 0, -1, -1, -1 };

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
		return new int[] { 0, 0, PANEL_LENGTH, 0, 1, 1 };
	}

	@Override
	public int[][] getAllDimensions() {
		// Separate the cap so the placement preview does not reserve blocks beside it.
		return new int[][] {
			{ 0, 0, 0, 0, 0, 0 },
			{ 0, 0, PANEL_LENGTH - 1, 0, 1, 1, -1, 0, 0 }
		};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	private List<BlockPos> panelBlocks(int x, int y, int z, ForgeDirection facing, int rotation) {
		List<BlockPos> blocks = new ArrayList<>(PANEL_LENGTH * PANEL_WIDTH);
		// Local model X points toward the port; local Z spans the panel's width.
		int acrossX = -facing.offsetZ;
		int acrossZ = facing.offsetX;
		// A 180-degree turn swaps the outer rows without changing the occupied blocks.
		// The low two bits map the eight 45-degree poses onto four footprint shapes.
		int shape = rotation & 3;
		for(int length = 1; length <= PANEL_LENGTH; length++) {
			for(int width = -1; width <= 1; width++) {
				blocks.add(new BlockPos(x - facing.offsetX * length + acrossX * width * HORIZONTAL_OFFSETS[shape],
						y + width * VERTICAL_OFFSETS[shape], z - facing.offsetZ * length + acrossZ * width * HORIZONTAL_OFFSETS[shape]));
			}
		}
		return blocks;
	}

	private boolean isPanelBlock(TileEntityChungusSolar solar, ForgeDirection facing, int x, int y, int z) {
		int dx = x - solar.xCoord, dy = y - solar.yCoord, dz = z - solar.zCoord;
		int length = -dx * facing.offsetX - dz * facing.offsetZ;
		if(length < 1 || length > PANEL_LENGTH) return false;
		int across = -dx * facing.offsetZ + dz * facing.offsetX;
		// Use the same half-turn symmetry as panelBlocks when checking ownership.
		int shape = solar.panelRotation & 3;
		for(int width = -1; width <= 1; width++) {
			if(across == width * HORIZONTAL_OFFSETS[shape] && dy == width * VERTICAL_OFFSETS[shape]) return true;
		}
		return false;
	}

	public List<BlockPos> getSolarCellBlocks(TileEntityChungusSolar solar) {
		List<BlockPos> blocks = panelBlocks(solar.xCoord, solar.yCoord, solar.zCoord,
				ForgeDirection.getOrientation(solar.getBlockMetadata() - offset), solar.panelRotation);
		// The row nearest the cap is the support arm, leaving fifteen rows of solar cells.
		return blocks.subList(PANEL_WIDTH, blocks.size());
	}

	@Override
	protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
		if(!world.blockExists(x, y, z)) return false;
		if(!world.getBlock(x, y, z).isReplaceable(world, x, y, z)) return false;
		for(BlockPos pos : panelBlocks(x, y, z, dir, 0)) {
			if(!world.blockExists(pos.getX(), pos.getY(), pos.getZ())) return false;
			if(!world.getBlock(pos.getX(), pos.getY(), pos.getZ()).isReplaceable(world, pos.getX(), pos.getY(), pos.getZ())) return false;
		}
		return true;
	}

	public void validateStructure(TileEntityChungusSolar solar) {
		World world = solar.getWorldObj();
		ForgeDirection facing = ForgeDirection.getOrientation(solar.getBlockMetadata() - offset);
		for(BlockPos pos : panelBlocks(solar.xCoord, solar.yCoord, solar.zCoord, facing, solar.panelRotation)) {
			// An unloaded cell is unknown, not missing; check it when its chunk is loaded again.
			if(!world.blockExists(pos.getX(), pos.getY(), pos.getZ())) continue;
			if(world.getBlock(pos.getX(), pos.getY(), pos.getZ()) != this
					|| world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ()) != facing.getOpposite().ordinal() + extra) {
				world.setBlockToAir(solar.xCoord, solar.yCoord, solar.zCoord);
				return;
			}
		}
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
		if(world instanceof World && !((World) world).blockExists(x, y, z)) return null;
		if(world.getBlock(x, y, z) != this) return null;
		int meta = world.getBlockMetadata(x, y, z);
		if(meta >= 12) return new int[] { x, y, z };
		if(meta < extra) return null;
		return findPanelCore(world, x, y, z, meta);
	}

	private int[] findPanelCore(IBlockAccess world, int x, int y, int z, int meta) {
		return findPanelCore(world, x, y, z, meta, false);
	}

	private int[] findPanelCore(IBlockAccess world, int x, int y, int z, int meta, boolean loadChunks) {
		ForgeDirection facing = ForgeDirection.getOrientation(meta - extra).getOpposite();
		for(int length = 1; length <= PANEL_LENGTH; length++) {
			for(int dy = -1; dy <= 1; dy++) {
				for(int width = -1; width <= 1; width++) {
					int cx = x + facing.offsetX * length - facing.offsetZ * width;
					int cy = y + dy;
					int cz = z + facing.offsetZ * length + facing.offsetX * width;
					if(cy < 0 || cy >= 256) continue;
					// Cleanup must resolve ownership even when the cap's chunk is unloaded.
					if(loadChunks && world instanceof World && !((World) world).blockExists(cx, cy, cz)) {
						((World) world).getChunkFromBlockCoords(cx, cz);
					}
					if(isPanelCore(world, cx, cy, cz, facing, x, y, z)) {
						return new int[] { cx, cy, cz };
					}
				}
			}
		}
		return null;
	}

	private boolean isPanelCore(IBlockAccess world, int cx, int cy, int cz, ForgeDirection facing, int x, int y, int z) {
		if(world instanceof World && !((World) world).blockExists(cx, cy, cz)) return false;
		if(world.getBlock(cx, cy, cz) != this || world.getBlockMetadata(cx, cy, cz) != facing.ordinal() + offset) return false;
		TileEntity te = world.getTileEntity(cx, cy, cz);
		return te instanceof TileEntityChungusSolar && !te.isInvalid() && isPanelBlock((TileEntityChungusSolar) te, facing, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		if(safeRem || world.isRemote) return;
		removeOrphan(world, x, y, z);
	}

	private void removeOrphan(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if(meta < extra || meta >= 12) return;
		if(findPanelCore(world, x, y, z, meta) != null) return;

		ForgeDirection facing = ForgeDirection.getOrientation(meta - extra).getOpposite();
		int startX = x + facing.offsetX, startZ = z + facing.offsetZ;
		int endX = x + facing.offsetX * PANEL_LENGTH, endZ = z + facing.offsetZ * PANEL_LENGTH;
		// Every possible cap must be loaded before a failed lookup proves this is an orphan.
		if(!world.checkChunksExist(Math.min(startX, endX) - Math.abs(facing.offsetZ), y - 1,
				Math.min(startZ, endZ) - Math.abs(facing.offsetX), Math.max(startX, endX) + Math.abs(facing.offsetZ),
				y + 1, Math.max(startZ, endZ) + Math.abs(facing.offsetX))) return;

		boolean previous = safeRem;
		safeRem = true;
		try {
			world.setBlockToAir(x, y, z);
		} finally {
			safeRem = previous;
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if(!safeRem && !world.isRemote) {
			if(meta >= 12) {
				// Chunk.setBlock has replaced the core with air, but its TE is still available here.
				TileEntity te = world.getTileEntity(x, y, z);
				if(te instanceof TileEntityChungusSolar) {
					ForgeDirection facing = ForgeDirection.getOrientation(meta - offset);
					List<BlockPos> blocks = panelBlocks(x, y, z, facing, ((TileEntityChungusSolar) te).panelRotation);
					boolean previous = safeRem;
					safeRem = true;
					try {
						// Load the complete footprint first; blockExists avoids reloading the same chunk.
						for(BlockPos pos : blocks) {
							if(!world.blockExists(pos.getX(), pos.getY(), pos.getZ())) {
								world.getChunkFromBlockCoords(pos.getX(), pos.getZ());
							}
						}
						int dummyMeta = facing.getOpposite().ordinal() + extra;
						for(BlockPos pos : blocks) {
							if(world.getBlock(pos.getX(), pos.getY(), pos.getZ()) != this
									|| world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ()) != dummyMeta) continue;
							int[] owner = findPanelCore(world, pos.getX(), pos.getY(), pos.getZ(), dummyMeta);
							if(owner == null || owner[0] == x && owner[1] == y && owner[2] == z) {
								world.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
							}
						}
					} finally {
						safeRem = previous;
					}
				}
			} else if(meta >= extra) {
				// The dummy is already gone. Let the core's normal callback remove its footprint.
				int[] core = findPanelCore(world, x, y, z, meta, true);
				if(core != null) world.setBlockToAir(core[0], core[1], core[2]);
			}
		}
		// Suppress the base class's face-connected teardown; it cannot follow diagonal rows.
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
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		// Harvesting a dummy must find an unloaded cap before deciding whether to drop an array.
		// True orphans have no drop, so cleaning their remnants cannot duplicate the item.
		int[] core = metadata >= 12 ? findCore(world, x, y, z)
				: metadata >= extra ? findPanelCore(world, x, y, z, metadata, !world.isRemote) : null;
		if(core == null) return new ArrayList<>();
		TileEntity te = world.getTileEntity(core[0], core[1], core[2]);
		if(!(te instanceof TileEntityChungusSolar) || te.isInvalid()) return new ArrayList<>();
		return super.getDrops(world, x, y, z, metadata, fortune);
	}

	@Override
	protected ItemStack createStackedBlock(int metadata) {
		// Automated silk-touch harvesting must not bypass the dummy ownership check in getDrops.
		return metadata >= 12 ? super.createStackedBlock(metadata) : null;
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
		validateStructure(solar);
		if(solar.isInvalid()) return false;
		ForgeDirection facing = ForgeDirection.getOrientation(solar.getBlockMetadata() - offset);
		int nextRotation = (solar.panelRotation + 1) % PANEL_ROTATIONS;
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
		world.markBlockForUpdate(pos[0], pos[1], pos[2]);
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
