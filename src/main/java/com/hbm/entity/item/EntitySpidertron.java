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
		public final Vec3 poleTarget;
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
			this.poleTarget = Vec3.createVectorHelper(this.knee.xCoord, this.knee.yCoord, this.knee.zCoord);
			// Shared joint vectors keep both segments connected as IK updates them.
			this.upper = new Segment(this.hip, this.knee);
			this.lower = new Segment(this.knee, this.foot);
			this.yaw = yaw;
			this.pitch = pitch;
		}

		public void solve(Vec3 target) {
			solveTwoBone(this.hip, target, this.poleTarget, this.upper.length, this.lower.length, this.knee, this.foot);
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
		if (!Double.isFinite(upperLength) || !Double.isFinite(lowerLength) || upperLength <= 0.0D || lowerLength <= 0.0D) {
			throw new IllegalArgumentException("Bone lengths must be finite and positive");
		}

		// All positions must use the same coordinate system. Read inputs before writing outputs.
		double hx = hip.xCoord;
		double hy = hip.yCoord;
		double hz = hip.zCoord;
		double dx = target.xCoord - hx;
		double dy = target.yCoord - hy;
		double dz = target.zCoord - hz;
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (distance > 1.0E-8D) {
			dx /= distance;
			dy /= distance;
			dz /= distance;
		} else {
			// A coincident target has no direction; use a deterministic downward axis.
			dx = 0.0D;
			dy = -1.0D;
			dz = 0.0D;
		}

		// Project the pole onto the plane perpendicular to the hip-to-foot axis.
		double bx = poleTarget.xCoord - hx;
		double by = poleTarget.yCoord - hy;
		double bz = poleTarget.zCoord - hz;
		double projection = bx * dx + by * dy + bz * dz;
		bx -= projection * dx;
		by -= projection * dy;
		bz -= projection * dz;
		double bendLength = Math.sqrt(bx * bx + by * by + bz * bz);
		if (bendLength < 1.0E-8D) {
			// Choose a nonparallel axis when the pole cannot define a bend plane.
			bx = Math.abs(dy) < 0.9D ? 0.0D : 1.0D;
			by = Math.abs(dy) < 0.9D ? 1.0D : 0.0D;
			bz = 0.0D;
			projection = bx * dx + by * dy;
			bx -= projection * dx;
			by -= projection * dy;
			bz -= projection * dz;
			bendLength = Math.sqrt(bx * bx + by * by + bz * bz);
		}
		bx /= bendLength;
		by /= bendLength;
		bz /= bendLength;

		double reach = Math.max(Math.abs(upperLength - lowerLength), Math.min(distance, upperLength + lowerLength));
		// Law of cosines gives the knee's distance along the target axis.
		// Equal bones can fold completely when the target is exactly at the hip.
		double along = reach == 0.0D ? 0.0D
				: (upperLength * upperLength - lowerLength * lowerLength + reach * reach) / (2.0D * reach);
		double bend = Math.sqrt(Math.max(0.0D, upperLength * upperLength - along * along));
		kneeOut.xCoord = hx + dx * along + bx * bend;
		kneeOut.yCoord = hy + dy * along + by * bend;
		kneeOut.zCoord = hz + dz * along + bz * bend;
		footOut.xCoord = hx + dx * reach;
		footOut.yCoord = hy + dy * reach;
		footOut.zCoord = hz + dz * reach;
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
			this.updateFeet();
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

	private void updateFeet() {
		double heading = Math.toRadians(this.rotationYaw);
		double cos = Math.cos(heading);
		double sin = Math.sin(heading);
		for (Leg leg : this.legs) {
			// Rotate the chassis-local rest position into the world before tracing.
			double x = this.posX + leg.footRest.xCoord * cos - leg.footRest.zCoord * sin;
			double z = this.posZ + leg.footRest.xCoord * sin + leg.footRest.zCoord * cos;
			Vec3 start = Vec3.createVectorHelper(x, this.posY + this.height, z);
			Vec3 end = Vec3.createVectorHelper(x, this.posY - GROUND_SEARCH_DISTANCE, z);
			MovingObjectPosition hit = this.worldObj.func_147447_a(start, end, false, true, false);
			if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
				// The ray is vertical, so local X/Z stay fixed; only Y changes.
				leg.solve(Vec3.createVectorHelper(leg.footRest.xCoord, hit.hitVec.yCoord - this.posY, leg.footRest.zCoord));
			} else {
				leg.solve(leg.footRest);
			}
		}
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
