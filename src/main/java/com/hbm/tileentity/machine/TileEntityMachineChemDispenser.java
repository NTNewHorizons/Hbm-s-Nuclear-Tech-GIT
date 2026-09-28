package com.hbm.tileentity.machine;

import com.hbm.inventory.chemistry.ChemReagent;
import com.hbm.inventory.chemistry.ChemReagents;
import com.hbm.inventory.chemistry.ChemicalReaction;
import com.hbm.inventory.chemistry.ChemicalReactions;
import com.hbm.inventory.chemistry.ReagentHolder;
import com.hbm.inventory.container.ContainerChemDispenser;
import com.hbm.inventory.gui.GUIChemDispenser;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineChemDispenser extends TileEntityMachineBase implements IEnergyReceiverMK2, IGUIProvider {

	public static final long MAX_POWER = 100_000L;
	public static final long POWER_PER_UNIT = 100L;
	public final ReagentHolder holder = new ReagentHolder(100D);

	private long power;
	private int dispenseAmount = 5;
	private String activeReaction = "";
	private int inverseNoticeTicks;

	public TileEntityMachineChemDispenser() {
		super(0);
	}

	@Override
	public String getName() {
		return "container.machineChemDispenser";
	}

	@Override
	public void updateEntity() {
		if(worldObj.isRemote) return;

		if(worldObj.getTotalWorldTime() % 20 == 0) {
			for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
			}
		}

		double inverseBefore = holder.getAmount(ChemReagents.TOXIC_SLURRY);
		ChemicalReaction reaction = ChemicalReactions.process(holder);
		activeReaction = reaction == null ? "" : reaction.translationKey;
		if(holder.getAmount(ChemReagents.TOXIC_SLURRY) > inverseBefore) inverseNoticeTicks = 60;
		if(inverseNoticeTicks > 0) inverseNoticeTicks--;

		if(reaction != null) markChanged();
		this.networkPackNT(32);
	}

	@Override
	public void handleButtonPacket(int value, int action) {
		if(action == 0) dispense(value);
		if(action == 1) {
			int[] amounts = {1, 5, 10};
			if(value >= 0 && value < amounts.length) dispenseAmount = amounts[value];
		}
		if(action == 2) {
			holder.clear();
			activeReaction = "";
			inverseNoticeTicks = 0;
		}
		markDirty();
	}

	private void dispense(int reagentIndex) {
		if(reagentIndex < 0 || reagentIndex >= ChemReagents.dispensable().size()) return;
		ChemReagent reagent = ChemReagents.dispensable().get(reagentIndex);
		long cost = dispenseAmount * POWER_PER_UNIT;
		if(power < cost || holder.getCapacity() - holder.getTotalAmount() + 0.0001D < dispenseAmount) return;
		if(holder.add(reagent, dispenseAmount) >= dispenseAmount) power -= cost;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		power = nbt.getLong("power");
		dispenseAmount = nbt.getInteger("dispenseAmount");
		if(dispenseAmount != 1 && dispenseAmount != 5 && dispenseAmount != 10) dispenseAmount = 5;
		holder.readFromNBT(nbt, "reagents");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setInteger("dispenseAmount", dispenseAmount);
		holder.writeToNBT(nbt, "reagents");
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeByte(dispenseAmount);
		ByteBufUtils.writeUTF8String(buf, activeReaction);
		buf.writeByte(inverseNoticeTicks);
		holder.writeToBuffer(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		dispenseAmount = buf.readUnsignedByte();
		activeReaction = ByteBufUtils.readUTF8String(buf);
		inverseNoticeTicks = buf.readUnsignedByte();
		holder.readFromBuffer(buf);
	}

	public int getDispenseAmount() { return dispenseAmount; }
	public String getActiveReaction() { return activeReaction; }
	public boolean isShowingInverseNotice() { return inverseNoticeTicks > 0; }

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = Math.max(0, Math.min(MAX_POWER, power)); }
	@Override public long getMaxPower() { return MAX_POWER; }

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerChemDispenser(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIChemDispenser(this);
	}
}
