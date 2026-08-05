package com.hbm.blocks.network;

import java.util.List;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import api.hbm.energymk2.CableProperties;
import api.hbm.energymk2.VoltageTier;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.network.TileEntityVoltageCable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockVoltageCable extends BlockCable implements ITooltipProvider, IToolable {

	private static final double CORE_MIN = 5.5D / 16D;
	private static final double CORE_MAX = 10.5D / 16D;

	private final CableProperties properties;

	public BlockVoltageCable(Material material, CableProperties properties) {
		super(material);
		this.properties = properties;
	}

	public CableProperties getProperties() {
		return properties;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityVoltageCable();
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block changedBlock) {
		super.onNeighborBlockChange(world, x, y, z, changedBlock);
		if(world.isRemote) return;
		TileEntity tile = world.getTileEntity(x, y, z);
		if(tile instanceof TileEntityVoltageCable) {
			((TileEntityVoltageCable) tile).clearCutsForChangedNeighbours();
		}
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;
		if(world.isRemote) return true;

		TileEntity tile = world.getTileEntity(x, y, z);
		if(!(tile instanceof TileEntityVoltageCable)) return false;

		TileEntityVoltageCable cable = (TileEntityVoltageCable) tile;
		return cable.toggleConnection(getClickedConnection(cable, side, hitX, hitY, hitZ));
	}

	private ForgeDirection getClickedConnection(TileEntityVoltageCable cable, int side, float hitX, float hitY, float hitZ) {
		ForgeDirection result = null;
		double distance = 0D;

		if(hitX > CORE_MAX && cable.isConnected(ForgeDirection.EAST)) { result = ForgeDirection.EAST; distance = hitX - CORE_MAX; }
		if(hitX < CORE_MIN && cable.isConnected(ForgeDirection.WEST) && CORE_MIN - hitX > distance) { result = ForgeDirection.WEST; distance = CORE_MIN - hitX; }
		if(hitY > CORE_MAX && cable.isConnected(ForgeDirection.UP) && hitY - CORE_MAX > distance) { result = ForgeDirection.UP; distance = hitY - CORE_MAX; }
		if(hitY < CORE_MIN && cable.isConnected(ForgeDirection.DOWN) && CORE_MIN - hitY > distance) { result = ForgeDirection.DOWN; distance = CORE_MIN - hitY; }
		if(hitZ > CORE_MAX && cable.isConnected(ForgeDirection.SOUTH) && hitZ - CORE_MAX > distance) { result = ForgeDirection.SOUTH; distance = hitZ - CORE_MAX; }
		if(hitZ < CORE_MIN && cable.isConnected(ForgeDirection.NORTH) && CORE_MIN - hitZ > distance) { result = ForgeDirection.NORTH; }

		return result == null ? ForgeDirection.getOrientation(side) : result;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		setBlockBounds(
				connectsOn(world, x, y, z, ForgeDirection.EAST),
				connectsOn(world, x, y, z, ForgeDirection.WEST),
				connectsOn(world, x, y, z, ForgeDirection.UP),
				connectsOn(world, x, y, z, ForgeDirection.DOWN),
				connectsOn(world, x, y, z, ForgeDirection.SOUTH),
				connectsOn(world, x, y, z, ForgeDirection.NORTH));
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if(!(tile instanceof TileEntityVoltageCable)) return super.collisionRayTrace(world, x, y, z, startVec, endVec);

		TileEntityVoltageCable cable = (TileEntityVoltageCable) tile;
		MovingObjectPosition closest = getIntercept(getCoreBox(x, y, z), startVec, endVec, null);
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if(cable.isConnected(direction)) {
				closest = getIntercept(getArmBox(direction, x, y, z), startVec, endVec, closest);
			}
		}
		return closest == null ? null : new MovingObjectPosition(x, y, z, closest.sideHit, closest.hitVec);
	}

	private MovingObjectPosition getIntercept(AxisAlignedBB box, Vec3 startVec, Vec3 endVec, MovingObjectPosition closest) {
		MovingObjectPosition intercept = box.calculateIntercept(startVec, endVec);
		if(intercept == null) return closest;
		if(closest == null || startVec.distanceTo(intercept.hitVec) < startVec.distanceTo(closest.hitVec)) return intercept;
		return closest;
	}

	private AxisAlignedBB getCoreBox(int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + CORE_MIN, y + CORE_MIN, z + CORE_MIN, x + CORE_MAX, y + CORE_MAX, z + CORE_MAX);
	}

	private AxisAlignedBB getArmBox(ForgeDirection direction, int x, int y, int z) {
		double minX = x + CORE_MIN;
		double minY = y + CORE_MIN;
		double minZ = z + CORE_MIN;
		double maxX = x + CORE_MAX;
		double maxY = y + CORE_MAX;
		double maxZ = z + CORE_MAX;

		switch(direction) {
		case EAST: maxX = x + 1D; break;
		case WEST: minX = x; break;
		case UP: maxY = y + 1D; break;
		case DOWN: minY = y; break;
		case SOUTH: maxZ = z + 1D; break;
		case NORTH: minZ = z; break;
		default: break;
		}
		return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private boolean connectsOn(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof TileEntityVoltageCable && ((TileEntityVoltageCable) tile).isConnected(direction);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(net.minecraft.item.ItemStack stack, EntityPlayer player, List list, boolean ext) {
		list.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("hbm.voltage.generic") + ": " + VoltageTier.format(properties.voltage));
		list.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocalFormatted("hbm.voltage.amperageLimit", properties.maxEnergyPerTick));
		list.add(EnumChatFormatting.RED + StatCollector.translateToLocalFormatted("hbm.voltage.loss", properties.lossPerBlock));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("hbm.voltage.wireCutterHint"));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("hbm.voltage.coreHint"));
	}
}
