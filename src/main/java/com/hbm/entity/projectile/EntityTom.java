package com.hbm.entity.projectile;

import com.hbm.config.SpaceConfig;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.logic.EntityTomBlast;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.AnnouncementPacket;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class EntityTom extends EntityThrowable implements IEntityAdditionalSpawnData {

	private static final int ANNOUNCEMENT_IMPACT_TICKS = 1140;
	private static final int SPAWN_HEIGHT = 600;
	private static final int DEFAULT_GROUND_Y = 64;
	private static final int MIN_FALL_DISTANCE = 100;
	private static final int FALLBACK_DISTANCE = 500;
	private static final double DEFAULT_FALL_SPEED = 0.5;
	private static final double MIN_FALL_SPEED = 0.35;
	private static final double MAX_FALL_SPEED = 0.9;
	private static final int MIN_DETONATION_Y = 10;
	private static final int DESTRUCTION_RANGE = 600;
	private static final int CLOUD_DURATION = 500;
	private static final float ANNOUNCEMENT_VOLUME = 10000.0F;

	private boolean isAnnouncementMode = false;
	private boolean hasPlayedAnnouncement = false;
	private double announcementFallSpeed = 0;

	public EntityTom(World p_i1582_1_) {
		super(p_i1582_1_);
		this.ignoreFrustumCheck = true;
	}

	public EntityTom setAnnouncementMode(boolean announcement) {
		this.isAnnouncementMode = announcement;
		return this;
	}

	private boolean isAnnouncement() {
		return this.isAnnouncementMode || this.worldObj.provider.dimensionId == SpaceConfig.moonDimension;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeBoolean(this.isAnnouncementMode);
		buffer.writeBoolean(this.hasPlayedAnnouncement);
		buffer.writeDouble(this.announcementFallSpeed);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		this.isAnnouncementMode = buffer.readBoolean();
		this.hasPlayedAnnouncement = buffer.readBoolean();
		this.announcementFallSpeed = buffer.readDouble();
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		this.isAnnouncementMode = nbt.getBoolean("isAnnouncement");
		this.hasPlayedAnnouncement = nbt.getBoolean("hasPlayedAnnouncement");
		this.announcementFallSpeed = nbt.getDouble("announcementFallSpeed");
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setBoolean("isAnnouncement", this.isAnnouncementMode);
		nbt.setBoolean("hasPlayedAnnouncement", this.hasPlayedAnnouncement);
		nbt.setDouble("announcementFallSpeed", this.announcementFallSpeed);
	}

	@Override
	public void onUpdate() {
		this.ticksExisted++;

		this.lastTickPosX = this.prevPosX = posX;
		this.lastTickPosY = this.prevPosY = posY;
		this.lastTickPosZ = this.prevPosZ = posZ;

		boolean isAnnouncement = isAnnouncement();

		if(isAnnouncement && this.announcementFallSpeed == 0 && !this.worldObj.isRemote) {
			int ix = MathHelper.floor_double(this.posX);
			int iz = MathHelper.floor_double(this.posZ);
			int h = this.worldObj.getHeightValue(ix, iz);
			int groundY = DEFAULT_GROUND_Y;
			if(h > 5) {
				groundY = h;
			} else {
				for(int y = 250; y > 5; y--) {
					if(this.worldObj.getBlock(ix, y, iz) != Blocks.air) {
						groundY = y;
						break;
					}
				}
			}
			double distance = this.posY - groundY;
			if(distance < MIN_FALL_DISTANCE) distance = SPAWN_HEIGHT - groundY;
			if(distance < MIN_FALL_DISTANCE) distance = FALLBACK_DISTANCE;
			this.announcementFallSpeed = distance / (double) ANNOUNCEMENT_IMPACT_TICKS;
			if(this.announcementFallSpeed < MIN_FALL_SPEED) this.announcementFallSpeed = MIN_FALL_SPEED;
			if(this.announcementFallSpeed > MAX_FALL_SPEED) this.announcementFallSpeed = MAX_FALL_SPEED;
		}

		if(isAnnouncement) {
			if(!this.hasPlayedAnnouncement && this.ticksExisted <= 5 && !this.worldObj.isRemote) {
				PacketDispatcher.wrapper.sendToDimension(new AnnouncementPacket(), 0);
				PacketDispatcher.wrapper.sendToDimension(new AnnouncementPacket(), SpaceConfig.moonDimension);
				this.hasPlayedAnnouncement = true;
			}
		} else {
			if(this.ticksExisted % 100 == 0) {
				worldObj.playSoundEffect(posX, posY, posZ, "hbm:alarm.chime", ANNOUNCEMENT_VOLUME, 1.0F);
			}
		}

		if(isAnnouncement) {
			if(this.announcementFallSpeed != 0) {
				motionY = -this.announcementFallSpeed;
			} else {
				motionY = -DEFAULT_FALL_SPEED;
			}
		} else {
			motionY = -DEFAULT_FALL_SPEED;
		}

		this.setPosition(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);

		if(!this.worldObj.isRemote) {
			this.worldObj.getChunkProvider().loadChunk(MathHelper.floor_double(this.posX) >> 4, MathHelper.floor_double(this.posZ) >> 4);
		}

		if(this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) != Blocks.air || this.posY < MIN_DETONATION_Y) {
			if(!this.worldObj.isRemote) {
				EntityTomBlast tom = new EntityTomBlast(worldObj);
				tom.posX = posX;
				tom.posY = posY;
				tom.posZ = posZ;
				tom.destructionRange = DESTRUCTION_RANGE;
				worldObj.spawnEntityInWorld(tom);

				EntityCloudTom cloud = new EntityCloudTom(worldObj, CLOUD_DURATION);
				cloud.setLocationAndAngles(posX, posY, posZ, 0, 0);
				worldObj.spawnEntityInWorld(cloud);
			}
			this.setDead();
		}
	}

	@Override
	protected void onImpact(MovingObjectPosition p_70184_1_) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 500000;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float p_70070_1_) {
		return 15728880;
	}

	@Override
	public float getBrightness(float p_70013_1_) {
		return 1.0F;
	}
}
