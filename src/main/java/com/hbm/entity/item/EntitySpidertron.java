package com.hbm.entity.item;

import java.util.concurrent.atomic.AtomicReference;

import com.hbm.util.BufferUtil;
import com.hbm.util.Vec3NT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.EntityBufPacket;
import com.hbm.tileentity.IBufPacketReceiver;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntitySpidertron extends Entity implements IBufPacketReceiver, IEntityAdditionalSpawnData {

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

		public Leg(Leg other) {
			this(new Vec3NT(other.hipOffset));
			this.hip = new Vec3NT(other.hip);
			this.knee = new Vec3NT(other.knee);
			this.foot = new Vec3NT(other.foot);
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

		public void writeToBuf(ByteBuf buf) {
			BufferUtil.writeVec3(buf, this.hip);
			BufferUtil.writeVec3(buf, this.knee);
			BufferUtil.writeVec3(buf, this.foot);
		}

		public static Leg readFromBuf(ByteBuf buf, Vec3NT hipOffset) {
			Leg leg = new Leg(new Vec3NT(hipOffset));
			leg.hip = new Vec3NT(BufferUtil.readVec3(buf));
			leg.knee = new Vec3NT(BufferUtil.readVec3(buf));
			leg.foot = new Vec3NT(BufferUtil.readVec3(buf));
			return leg;
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

	private static class Pose {
		final Vec3 position;
		final double yaw;
		final double pitch;
		final Leg[] legs;

		Pose(Vec3 position, double yaw, double pitch, Leg[] legs) {
			this.position = position;
			this.yaw = yaw;
			this.pitch = pitch;
			this.legs = legs;
		}

		void writeToBuf(ByteBuf buf) {
			BufferUtil.writeVec3(buf, this.position);
			buf.writeDouble(this.yaw);
			buf.writeDouble(this.pitch);
			for (Leg leg : this.legs) {
				leg.writeToBuf(buf);
			}
		}

		static Pose readFromBuf(ByteBuf buf, Leg[] templates) {
			Vec3 position = BufferUtil.readVec3(buf);
			double yaw = buf.readDouble();
			double pitch = buf.readDouble();
			Leg[] legs = new Leg[templates.length];
			for (int i = 0; i < legs.length; i++) {
				legs[i] = Leg.readFromBuf(buf, templates[i].hipOffset);
			}
			return new Pose(position, yaw, pitch, legs);
		}
	}

	// Network callbacks publish a complete snapshot, only the client tick applies it.
	private final AtomicReference<Pose> pendingPose = new AtomicReference<>();

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
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int ticks) {}

	@Override
	public void serialize(ByteBuf buf) {
		capturePose().writeToBuf(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.pendingPose.set(Pose.readFromBuf(buf, this.legs));
	}

	private Pose capturePose() {
		Leg[] poseLegs = new Leg[this.legs.length];
		for (int i = 0; i < this.legs.length; i++) {
			poseLegs[i] = new Leg(this.legs[i]);
		}
		return new Pose(new Vec3NT(this.posX, this.posY, this.posZ), this.rotationYaw, this.rotationPitch, poseLegs);
	}

	private void applyPendingPose() {
		Pose pose = this.pendingPose.getAndSet(null);
		if (pose == null) return;
		this.setPosition(pose.position.xCoord, pose.position.yCoord, pose.position.zCoord);
		this.setRotation((float) pose.yaw, (float) pose.pitch);
		this.prevRotationYaw = this.rotationYaw
				- MathHelper.wrapAngleTo180_float(this.rotationYaw - this.prevRotationYaw);
		for (int i = 0; i < this.legs.length; i++) {
			this.legs[i].hip = pose.legs[i].hip;
			this.legs[i].knee = pose.legs[i].knee;
			this.legs[i].foot = pose.legs[i].foot;
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buf) {
		serialize(buf);
	}

	@Override
	public void readSpawnData(ByteBuf buf) {
		deserialize(buf);
		applyPendingPose();
		this.lastTickPosX = this.prevPosX = this.posX;
		this.lastTickPosY = this.prevPosY = this.posY;
		this.lastTickPosZ = this.prevPosZ = this.posZ;
		this.prevRotationYaw = this.rotationYaw;
		this.prevRotationPitch = this.rotationPitch;
	}

	private void solveLegs() {
		for (Leg leg : this.legs) {
			leg.solve(new Vec3NT(this.posX, this.posY, this.posZ), this.rotationYaw);
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (this.worldObj.isRemote) {
			applyPendingPose();
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

		solveLegs();

		for (EntityPlayer player : ((WorldServer) this.worldObj).getEntityTracker().getTrackingPlayers(this)) {
			PacketDispatcher.wrapper.sendTo(new EntityBufPacket(this.getEntityId(), this), (EntityPlayerMP) player);
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
