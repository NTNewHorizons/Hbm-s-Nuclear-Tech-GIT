package com.hbm.entity.item;

import java.util.List;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.util.fauxpointtwelve.BlockPos;

import api.hbm.conveyor.IConveyorBelt;
import api.hbm.conveyor.IEnterableBlock;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.CranePartitioner;
import com.hbm.blocks.network.CraneSplitter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class EntityMovingConveyorObject extends Entity {
	
	protected int turnProgress;
	protected double syncPosX;
	protected double syncPosY;
	protected double syncPosZ;
	@SideOnly(Side.CLIENT) protected double velocityX;
	@SideOnly(Side.CLIENT) protected double velocityY;
	@SideOnly(Side.CLIENT) protected double velocityZ;

	public static final double HITBOX_RADIUS = 0.1875;

	public static final int CRAM_CHECK_TICKS = 1 * 20;
	public static final int CRAM_CHECK_LIMIT = 25;
	private boolean blocked = false;

	public static boolean isCrammed(World world, int x, int y, int z) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1).contract(HITBOX_RADIUS, HITBOX_RADIUS, HITBOX_RADIUS);
		List<EntityMovingConveyorObject> objs = world.getEntitiesWithinAABB(EntityMovingConveyorObject.class, box);
		return objs.size() > CRAM_CHECK_LIMIT;
	}

	public static ForgeDirection getConveyorOutputDirection(World world, Block block, int x, int y, int z, Vec3 itemPos) {
		if(block instanceof BlockConveyorBase) {
			return ((BlockConveyorBase) block).getOutputDirection(world, x, y, z);
		} else if(block instanceof CraneSplitter) {
			return ((CraneSplitter) block).getTravelDirection(world, x, y, z, itemPos).getOpposite();
		} else if(block instanceof CranePartitioner) {
			return ((CranePartitioner) block).getTravelDirection(world, x, y, z, itemPos).getOpposite();
		} else if(block instanceof IConveyorBelt) {
			return ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z)).getOpposite();
		}
		return ForgeDirection.UNKNOWN;
	}

	public EntityMovingConveyorObject(World world) {
		super(world);
		this.noClip = true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean canAttackWithItem() {
		return true;
	}

	@Override
	public boolean hitByEntity(Entity attacker) {

		if(attacker instanceof EntityPlayer) {
			this.setDead();
		}
		
		return false;
	}

	@Override
	protected boolean canTriggerWalking() {
		return true;
	}

	@Override
	public void onUpdate() {
		
		if(worldObj.isRemote) {
			if(this.turnProgress > 0) {
				double interpX = this.posX + (this.syncPosX - this.posX) / (double) this.turnProgress;
				double interpY = this.posY + (this.syncPosY - this.posY) / (double) this.turnProgress;
				double interpZ = this.posZ + (this.syncPosZ - this.posZ) / (double) this.turnProgress;
				--this.turnProgress;
				this.setPosition(interpX, interpY, interpZ);
			} else {
				this.setPosition(this.posX, this.posY, this.posZ);
			}
		}

		if(!worldObj.isRemote) {

			ticksExisted++;
			
			if(this.ticksExisted <= 5) {
				return;
			}

			final boolean firstUpdate = ticksExisted == 6;
			
			int blockX = (int) Math.floor(posX);
			int blockY = (int) Math.floor(posY);
			int blockZ = (int) Math.floor(posZ);

			Block block = worldObj.getBlock(blockX, blockY, blockZ);

			if((firstUpdate || ((ticksExisted + this.getEntityId()) % CRAM_CHECK_TICKS == 0)) && block instanceof IConveyorBelt) {
				ForgeDirection dir = getConveyorOutputDirection(worldObj, block, blockX, blockY, blockZ, Vec3.createVectorHelper(posX, posY, posZ));
				if(dir != ForgeDirection.UNKNOWN) {
					boolean isForwardCrammed = isCrammed(worldObj, blockX + dir.offsetX, blockY + dir.offsetY, blockZ + dir.offsetZ);

					if (blocked != isForwardCrammed) {
						AxisAlignedBB blockHere = AxisAlignedBB.getBoundingBox(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1).contract(HITBOX_RADIUS, HITBOX_RADIUS, HITBOX_RADIUS);
						List<EntityMovingConveyorObject> objsHere = worldObj.getEntitiesWithinAABB(EntityMovingConveyorObject.class, blockHere);

						for (EntityMovingConveyorObject obj : objsHere) {
							obj.blocked = isForwardCrammed;
						}
					}
				}
			}

			boolean isOnConveyor = block instanceof IConveyorBelt && ((IConveyorBelt) block).canItemStay(worldObj, blockX, blockY, blockZ, Vec3.createVectorHelper(posX, posY, posZ));
			
			if(!isOnConveyor) {
				
				if(onLeaveConveyor()) {
					return;
				}
			} else {
				
				Vec3 target = ((IConveyorBelt) block).getTravelLocation(worldObj, blockX, blockY, blockZ, Vec3.createVectorHelper(posX, posY, posZ), getMoveSpeed());
				this.motionX = target.xCoord - posX;
				this.motionY = target.yCoord - posY;
				this.motionZ = target.zCoord - posZ;
			}
			
			BlockPos lastPos = new BlockPos(posX, posY, posZ);
			if (!blocked) this.moveEntity(motionX, motionY, motionZ);
			BlockPos newPos = new BlockPos(posX, posY, posZ);
			
			if(!lastPos.equals(newPos)) {
				
				Block newBlock = worldObj.getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				
				if(newBlock instanceof IEnterableBlock) {
					
					ForgeDirection dir = ForgeDirection.UNKNOWN;

					if(lastPos.getX() > newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Library.POS_X;
					else if(lastPos.getX() < newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Library.NEG_X;
					else if(lastPos.getX() == newPos.getX() && lastPos.getY() > newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Library.POS_Y;
					else if(lastPos.getX() == newPos.getX() && lastPos.getY() < newPos.getY() && lastPos.getZ() == newPos.getZ()) dir = Library.NEG_Y;
					else if(lastPos.getX() == newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() > newPos.getZ()) dir = Library.POS_Z;
					else if(lastPos.getX() == newPos.getX() && lastPos.getY() == newPos.getY() && lastPos.getZ() < newPos.getZ()) dir = Library.NEG_Z;
					
					IEnterableBlock enterable = (IEnterableBlock) newBlock;
					enterBlock(enterable, newPos, dir);
					
				} else {
					
					if(!newBlock.getMaterial().isSolid()) {
						
						newBlock = worldObj.getBlock(newPos.getX(), newPos.getY() - 1, newPos.getZ());
						
						if(newBlock instanceof IEnterableBlock) {
							
							IEnterableBlock enterable = (IEnterableBlock) newBlock;
							enterBlockFalling(enterable, newPos);
						}
					}
				}
			}
		}
	}

	public abstract void enterBlock(IEnterableBlock enterable, BlockPos pos, ForgeDirection dir);
	
	public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
		this.enterBlock(enterable, pos.add(0, -1, 0), ForgeDirection.UP);
	}
	
	/**
	 * @return true if the update loop should end
	 */
	public abstract boolean onLeaveConveyor();
	
	public double getMoveSpeed() {
		return 0.0625D;
	}
	
	@SideOnly(Side.CLIENT)
	public void setVelocity(double motionX, double motionY, double motionZ) {
		this.velocityX = this.motionX = motionX;
		this.velocityY = this.motionY = motionY;
		this.velocityZ = this.motionZ = motionZ;
	}
	
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int theNumberThree) {
		this.syncPosX = x;
		this.syncPosY = y;
		this.syncPosZ = z;
		this.turnProgress = theNumberThree + 2; //use 4-ply for extra smoothness
		this.motionX = this.velocityX;
		this.motionY = this.velocityY;
		this.motionZ = this.velocityZ;
	}
}
