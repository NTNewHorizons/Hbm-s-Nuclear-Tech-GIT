package com.hbm.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntitySpidertron extends Entity {

	private static final double ACCELERATION = 0.1D;
	private static final double MAX_SPEED = 0.75D;
	private static final double HOVER_HEIGHT = 4.0D;
	private static final double GROUND_SEARCH_DISTANCE = 128.0D;

	public static class Leg {
		private static final double KNEE_HEIGHT = 1.5D;
		private static final double KNEE_OUTWARD_FRACTION = 0.65D;

		public static class Segment {
			public final Vec3 start;
			public final Vec3 end;
			// Rest length for the IK solver; moving a joint does not change it.
			public final double length;

			public Segment(Vec3 start, Vec3 end) {
				this.start = start;
				this.end = end;
				this.length = start.distanceTo(end);
			}
		}

		// Chassis-local offsets in blocks, with positive Y pointing up.
		public final Vec3 hip;
		public final Vec3 footRest;
		public final Vec3 knee;
		public final Vec3 foot;
		public final Segment upper;
		public final Segment lower;
		public double yaw;
		public double pitch;

		public Leg(double x, double y, double z, double yaw, double pitch, double footX, double footZ) {
			this.hip = Vec3.createVectorHelper(x, y, z);
			this.footRest = Vec3.createVectorHelper(footX, -HOVER_HEIGHT, footZ);
			this.knee = Vec3.createVectorHelper(
					x + (footX - x) * KNEE_OUTWARD_FRACTION,
					y + KNEE_HEIGHT,
					z + (footZ - z) * KNEE_OUTWARD_FRACTION);
			this.foot = Vec3.createVectorHelper(this.footRest.xCoord, this.footRest.yCoord, this.footRest.zCoord);
			// Shared joint vectors keep both segments connected as IK updates them.
			this.upper = new Segment(this.hip, this.knee);
			this.lower = new Segment(this.knee, this.foot);
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	public final Leg[] legs = new Leg[] {
			new Leg(0.75, 0, 1.0, 90, -35, 2.5, 3.0),
			new Leg(1.3, 0, 0.4, 90, -10, 3.5, 1.0),
			new Leg(1.3, 0, -0.4, 90, -10, 3.5, -1.0),
			new Leg(0.75, 0, -1.0, 90, -10, 2.5, -3.0),
			new Leg(-0.75, 0, 1.0, 90, -35, -2.5, 3.0),
			new Leg(-1.3, 0, 0.4, 90, -10, -3.5, 1.0),
			new Leg(-1.3, 0, -0.4, 90, -10, -3.5, -1.0),
			new Leg(-0.75, 0, -1.0, 90, -10, -2.5, -3.0),
	};

	public static void solveTwoBone(
			Vec3 hip,
			Vec3 target,
			Vec3 poleTarget,
			double upperLength,
			double lowerLength,
			Vec3 kneeOut,
			Vec3 footOut) {

	}

	private int interpolationTicks;
	private double targetX;
	private double targetY;
	private double targetZ;
	private float targetYaw;
	private float targetPitch;

	public EntitySpidertron(World world) {
		super(world);
		this.preventEntitySpawning = true;
		this.setSize(1, 1);
		this.stepHeight = 1;
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return this.boundingBox;
	}

	@Override
	public AxisAlignedBB getCollisionBox(Entity entity) {
		return entity == this.riddenByEntity ? null : entity.boundingBox;
	}

	@Override
	public boolean canBeCollidedWith() {
		return !this.isDead;
	}

	@Override
	public double getMountedYOffset() {
		return 0.0D;
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (!this.worldObj.isRemote && (this.riddenByEntity == null || this.riddenByEntity == player)) {
			player.mountEntity(this);
		}
		return true;
	}

	@Override
	public void updateRiderPosition() {
		if (this.riddenByEntity != null) {
			this.riddenByEntity.setPosition(this.posX,
					this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ);
		}
	}

	@Override
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int ticks) {
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
		this.targetYaw = yaw;
		this.targetPitch = pitch;
		this.interpolationTicks = Math.max(1, ticks);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (this.worldObj.isRemote) {
			if (this.interpolationTicks > 0) {
				double x = this.posX + (this.targetX - this.posX) / this.interpolationTicks;
				double y = this.posY + (this.targetY - this.posY) / this.interpolationTicks;
				double z = this.posZ + (this.targetZ - this.posZ) / this.interpolationTicks;
				float yaw = this.rotationYaw
						+ MathHelper.wrapAngleTo180_float(this.targetYaw - this.rotationYaw) / this.interpolationTicks;
				float pitch = this.rotationPitch + (this.targetPitch - this.rotationPitch) / this.interpolationTicks;
				--this.interpolationTicks;
				this.setPosition(x, y, z);
				this.setRotation(yaw, pitch);
			}
			return;
		}

		if (this.riddenByEntity instanceof EntityLivingBase) {
			EntityLivingBase rider = (EntityLivingBase) this.riddenByEntity;
			this.rotationYaw = rider.rotationYaw;
			double heading = Math.toRadians(this.rotationYaw);
			double forward = rider.moveForward;
			double strafe = rider.moveStrafing;
			double inputLength = Math.sqrt(forward * forward + strafe * strafe);
			if (inputLength > 1.0D) {
				forward /= inputLength;
				strafe /= inputLength;
			}
			this.motionX += (strafe * Math.cos(heading) - forward * Math.sin(heading)) * ACCELERATION;
			this.motionZ += (forward * Math.cos(heading) + strafe * Math.sin(heading)) * ACCELERATION;
		}

		double speed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
		if (speed > MAX_SPEED) {
			this.motionX *= MAX_SPEED / speed;
			this.motionZ *= MAX_SPEED / speed;
		}

		this.motionY = 0.0D;
		this.fallDistance = 0.0F;
		this.moveEntity(this.motionX, 0.0D, this.motionZ);
		this.snapToGround();
		this.motionX *= 0.9D;
		this.motionZ *= 0.9D;
		this.setRotation(this.rotationYaw, 0.0F);
	}

	private void snapToGround() {
		Vec3 start = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
		Vec3 end = Vec3.createVectorHelper(this.posX, this.posY - GROUND_SEARCH_DISTANCE, this.posZ);
		MovingObjectPosition hit = this.worldObj.func_147447_a(start, end, false, true, false);
		if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			this.setPosition(this.posX, hit.hitVec.yCoord + HOVER_HEIGHT, this.posZ);
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isEntityInvulnerable() || amount <= 0.0F)
			return false;
		if (!this.worldObj.isRemote) {
			if (this.riddenByEntity != null)
				this.riddenByEntity.mountEntity(null);
			this.setDead();
		}
		return true;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
	}
}
