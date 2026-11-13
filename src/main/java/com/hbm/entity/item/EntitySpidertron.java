package com.hbm.entity.item;

import com.hbm.util.Vec3NT;

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
	private static final double HEIGHT = 4.0D;
	private static final double KNEE_HEIGHT = 6.0D;
	private static final double FOOT_REST_OFFSET_MULTIPLIER = 4.0D;
	private static final double KNEE_REST_FRACTION = 0.35D;

	public static class Leg {
		public final Vec3NT hipOffset;
		public final Vec3NT kneeRestOffset;
		public final Vec3NT footRestOffset;
		public Vec3NT hip;
		public Vec3NT knee;
		public Vec3NT foot;

		public Leg(Vec3NT hipOffset) {
			this.hipOffset = hipOffset;
			this.hip = new Vec3NT(this.hipOffset);
			
			this.footRestOffset = new Vec3NT(this.hipOffset).multiply(FOOT_REST_OFFSET_MULTIPLIER);
			this.foot = new Vec3NT(this.footRestOffset);


			this.kneeRestOffset = new Vec3NT(this.footRestOffset).multiply(KNEE_REST_FRACTION);
			this.kneeRestOffset.yCoord = KNEE_HEIGHT;
			this.knee = new Vec3NT(this.kneeRestOffset);
		}

		/**
		 * Solve the IK for the leg
		 */
		public void solve(Vec3NT position, double yaw) {
			this.hip = new Vec3NT(position).add(new Vec3NT(this.hipOffset).rotateAroundYDeg(-yaw));
			this.foot = new Vec3NT(position).add(new Vec3NT(this.footRestOffset).rotateAroundYDeg(-yaw));
			this.knee = new Vec3NT(position).add(new Vec3NT(this.kneeRestOffset).rotateAroundYDeg(-yaw));
			/* Vec3NT footTarget = new Vec3NT(position).add(this.footRestOffset);

			// Solve for knee position
			Vec3NT hipToFoot = new Vec3NT(footTarget).add(new Vec3NT(this.hip).multiply(-1D));
			double hipToFootLength = hipToFoot.distanceTo(0, 0, 0);
			double kneeHeight = KNEE_HEIGHT;
			if (hipToFootLength < 1.0E-4D) {
				this.knee = new Vec3NT(this.hip);
				this.knee.yCoord += kneeHeight;
				this.foot = new Vec3NT(this.hip);
				return;
			}
			Vec3NT hipToFootDir = new Vec3NT(hipToFoot).multiply(1D / hipToFootLength);
			Vec3NT kneeOffset = new Vec3NT(-hipToFootDir.zCoord, 0, hipToFootDir.xCoord).normalizeSelf().multiply(kneeHeight);
			this.knee = new Vec3NT(this.hip).add(new Vec3NT(hipToFoot).multiply(0.5D)).add(kneeOffset);
			this.foot = footTarget; */
		}
	}

	public final Leg[] legs = new Leg[] {
			new Leg(new Vec3NT( 0.75D, 0.0D,  1.0D)),
			new Leg(new Vec3NT( 1.3D , 0.0D,  0.4D)),
			new Leg(new Vec3NT( 1.3D , 0.0D, -0.4D)),
			new Leg(new Vec3NT( 0.75D, 0.0D, -1.0D)),
			new Leg(new Vec3NT(-0.75D, 0.0D,  1.0D)),
			new Leg(new Vec3NT(-1.3D , 0.0D,  0.4D)),
			new Leg(new Vec3NT(-1.3D , 0.0D, -0.4D)),
			new Leg(new Vec3NT(-0.75D, 0.0D, -1.0D)),
	};

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

		for (EntitySpidertron.Leg leg : this.legs) {
			leg.solve(new Vec3NT(this.posX, this.posY, this.posZ), this.rotationYaw);
		}
	}

	private void snapToGround() {
		Vec3 start = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
		Vec3 end = Vec3.createVectorHelper(this.posX, this.posY - 128, this.posZ);
		MovingObjectPosition hit = this.worldObj.func_147447_a(start, end, false, true, false);
		if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			this.setPosition(this.posX, hit.hitVec.yCoord + HEIGHT, this.posZ);
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
