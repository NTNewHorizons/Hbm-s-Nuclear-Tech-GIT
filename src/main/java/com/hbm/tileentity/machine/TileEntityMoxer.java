package com.hbm.tileentity.machine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMoxer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMoxer;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.recipes.MoxerRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSources;
import com.hbm.items.machine.customfuel.EnumCustomFuel;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.main.NTMSounds;
import com.hbm.module.machine.ModuleMachineMoxer;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IMetalCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class TileEntityMoxer extends TileEntityMachineBase implements IGUIProvider, IConfigurableMachine, IMetalCopiable, IControlReceiver, IEnergyReceiverMK2, IUpgradeInfoProvider, IFluidStandardReceiverMK2 {

	public static final String MOXER_POOL = GenericRecipes.POOL_PREFIX_ALT + "moxer";

	public static int bufferCapacity = MaterialShapes.BLOCK.q(16);

	// dissolved isotopes, and how much of each one cycle takes
	public CustomFuelMix buffer = new CustomFuelMix();
	public CustomFuelMix perCycle = new CustomFuelMix();

	public FluidTank[] inputTanks;
	public long power;
	public long maxPower = 1_000_000;
	public boolean didProcess = false;
	public boolean autoMode;
	public Status status = Status.IDLE;

	public boolean frame = false;
	public int anim;
	public int prevAnim;
	private AudioWrapper audio;

	public ModuleMachineMoxer moxerModule;
	public UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

	public enum Status { IDLE, READY, RUNNING }

	public TileEntityMoxer() {
		super(12);
		this.inputTanks = new FluidTank[3];
		for(int i = 0; i < inputTanks.length; i++) this.inputTanks[i] = new FluidTank(Fluids.NONE, 16_000);

		this.moxerModule = new ModuleMachineMoxer(0, this, slots)
				.buffer(buffer, perCycle)
				.itemOutput(5)
				.fluidInput(inputTanks[0], inputTanks[1], inputTanks[2]);
	}

	protected PortDef[] cachedPorts;
	public PortDef[] getPorts() { if(cachedPorts == null) cachedPorts = TilePortShapes.moxer(xCoord, yCoord, zCoord, getBlockMetadata()); return cachedPorts; }

	@Override
	public String getName() {
		return "container.machineMoxer";
	}

	@Override
	public void updateEntity() {

		if(maxPower <= 0) this.maxPower = 1_000_000;

		if(!worldObj.isRemote) {

			this.setupPowerPorts(getPorts());
			this.setupFluidInPorts(getReceivingTanks(), PortDef.combine(getPorts()));
			this.updatePortPIFIFO();

			GenericRecipe recipe = moxerModule.getRecipe();
			if(recipe != null) {
				this.maxPower = recipe.power * 100;
			}
			this.maxPower = BobMathUtil.max(this.power, this.maxPower, 1_000_000);

			this.power = Library.chargeTEFromItems(slots, 0, power, maxPower);
			upgradeManager.checkSlots(slots, 2, 3);

			inputTanks[0].loadTank(6, 9, slots);
			inputTanks[1].loadTank(7, 10, slots);
			inputTanks[2].loadTank(8, 11, slots);

			double speed = 1D;
			double pow = 1D;

			speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
			speed += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

			pow -= Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
			pow += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
			pow += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

			if(dissolveInput()) this.markDirty();

			this.moxerModule.update(speed, pow, autoMode, slots[1]);
			this.didProcess = this.moxerModule.didProcess;
			if(this.moxerModule.markDirty) this.markDirty();

			if(didProcess) status = Status.RUNNING;
			else if(!autoMode && moxerModule.canProcess(moxerModule.getRecipe(), speed, pow)) status = Status.READY;
			else status = Status.IDLE;

			this.networkPackNT(100);

		} else {

			this.prevAnim = this.anim;
			if(this.didProcess) this.anim++;

			if(this.didProcess && worldObj.rand.nextInt(10) == 0) {
				// steam out of the fan, which sits centred on the core block
				worldObj.spawnParticle("cloud", xCoord + 0.1D + worldObj.rand.nextDouble() * 0.8D, yCoord + 2.5D, zCoord + 0.1D + worldObj.rand.nextDouble() * 0.8D, 0.0, 0.1, 0.0);
			}

			if(worldObj.getTotalWorldTime() % 20 == 0) {
				frame = !worldObj.getBlock(xCoord, yCoord + 3, zCoord).isAir(worldObj, xCoord, yCoord + 3, zCoord);
			}

			if(this.didProcess && MainRegistry.proxy.me().getDistance(xCoord, yCoord, zCoord) < 50) {
				if(audio == null) {
					audio = createAudioLoop();
					audio.startSound();
				} else if(!audio.isPlaying()) {
					audio = rebootAudio(audio);
				}
				audio.keepAlive();
				audio.updateVolume(this.getVolume(1F));

			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}
		}
	}

	// dissolves one item from the input slot into the buffer if it fits
	protected boolean dissolveInput() {
		ItemStack stack = slots[4];
		CustomFuelMix mix = CustomFuelSources.dissolve(stack);
		if(mix == null || buffer.total() + mix.total() > bufferCapacity) return false;
		buffer.add(mix);
		this.decrStackSize(4, 1);
		return true;
	}

	public boolean canDissolve(ItemStack stack) {
		CustomFuelMix mix = CustomFuelSources.dissolve(stack);
		return mix != null && buffer.total() + mix.total() <= bufferCapacity;
	}

	// isotopes in the buffer or still set to be used, in the order the GUI lists them
	public List<EnumCustomFuel> getListedIsotopes() {
		List<EnumCustomFuel> list = new ArrayList();
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) if(buffer.get(fuel) > 0 || perCycle.get(fuel) > 0) list.add(fuel);
		return list;
	}

	@Override public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound(NTMSounds.CENTRIFUGE_LOOP, xCoord, yCoord, zCoord, 1F, 15F, 1.0F, 20);
	}

	@Override public void onChunkUnload() {
		super.onChunkUnload();
		if(audio != null) { audio.stopSound(); audio = null; }
	}

	@Override public void invalidate() {
		super.invalidate();
		if(audio != null) { audio.stopSound(); audio = null; }
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		for(FluidTank tank : inputTanks) tank.serialize(buf);
		buf.writeLong(power);
		buf.writeLong(maxPower);
		buf.writeBoolean(didProcess);
		buf.writeBoolean(autoMode);
		buf.writeByte(status.ordinal());
		buffer.serialize(buf);
		perCycle.serialize(buf);
		this.moxerModule.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		for(FluidTank tank : inputTanks) tank.deserialize(buf);
		this.power = buf.readLong();
		this.maxPower = buf.readLong();
		this.didProcess = buf.readBoolean();
		this.autoMode = buf.readBoolean();
		this.status = Status.values()[buf.readByte()];
		copyInto(CustomFuelMix.deserialize(buf), buffer);
		copyInto(CustomFuelMix.deserialize(buf), perCycle);
		this.moxerModule.deserialize(buf);
	}

	// the module holds references to buffer and perCycle, so they get refilled instead of replaced
	private static void copyInto(CustomFuelMix from, CustomFuelMix to) {
		if(from == null) from = new CustomFuelMix();
		System.arraycopy(from.amounts, 0, to.amounts, 0, to.amounts.length);
		to.fresh = from.fresh;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		for(int i = 0; i < inputTanks.length; i++) inputTanks[i].readFromNBT(nbt, "t" + i);
		this.power = nbt.getLong("power");
		this.maxPower = nbt.getLong("maxPower");
		this.autoMode = nbt.getBoolean("autoMode");
		copyInto(CustomFuelMix.fromNBT(nbt.getCompoundTag("buffer")), buffer);
		copyInto(CustomFuelMix.fromNBT(nbt.getCompoundTag("perCycle")), perCycle);
		this.moxerModule.readFromNBT(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		for(int i = 0; i < inputTanks.length; i++) inputTanks[i].writeToNBT(nbt, "t" + i);
		nbt.setLong("power", power);
		nbt.setLong("maxPower", maxPower);
		nbt.setBoolean("autoMode", autoMode);
		nbt.setTag("buffer", buffer.writeNBT(new NBTTagCompound()));
		nbt.setTag("perCycle", perCycle.writeNBT(new NBTTagCompound()));
		this.moxerModule.writeToNBT(nbt);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return true; // battery
		if(slot == 1 && stack.getItem() == ModItems.blueprints) return MOXER_POOL.equals(ItemBlueprints.grabPool(stack));
		if(slot >= 2 && slot <= 3) return this.isUpgradeValid(stack);
		if(slot == 4) return canDissolve(stack);
		if(slot >= 6 && slot <= 8) return true; // input fluid
		return false;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 5 || (slot >= 9 && slot <= 11);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[] { 4, 5, 6, 7, 8, 9, 10, 11 };
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return maxPower; }

	@Override public FluidTank[] getReceivingTanks() { return inputTanks; }
	@Override public FluidTank[] getAllTanks() { return inputTanks; }

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerMoxer(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMoxer(player.inventory, this); }

	@Override public boolean hasPermission(EntityPlayer player) { return this.isUseableByPlayer(player); }

	@Override
	public void receiveControl(EntityPlayer player, NBTTagCompound data) {

		if(data.hasKey("isotope") && data.hasKey("amount")) {
			int isotope = data.getInteger("isotope");
			GenericRecipe recipe = moxerModule.getRecipe();
			if(isotope >= 0 && isotope < perCycle.amounts.length && recipe != null) {
				int others = perCycle.total() - perCycle.amounts[isotope];
				perCycle.amounts[isotope] = Math.max(0, Math.min(data.getInteger("amount"), ((MoxerRecipe) recipe).amount - others));
				this.markChanged();
			}
		}

		if(data.hasKey("autoMode")) {
			this.autoMode = data.getBoolean("autoMode");
			this.markChanged();
		}

		if(data.hasKey("index") && data.hasKey("selection")) {
			int index = data.getInteger("index");
			String selection = data.getString("selection");
			if(index == 0) {
				this.moxerModule.setRecipe(selection, false);
				for(int i = 0; i < perCycle.amounts.length; i++) perCycle.amounts[i] = 0;
				this.markChanged();
			}
		}
	}

	@Override
	public int[] getMatsToCopy() {
		List<Integer> types = new ArrayList();
		for(EnumCustomFuel fuel : getListedIsotopes()) types.add(fuel.stack.id);
		return BobMathUtil.intCollectionToArray(types);
	}

	@Override public String getConfigName() { return "moxer"; }
	@Override public void readIfPresent(JsonObject obj) { bufferCapacity = IConfigurableMachine.grab(obj, "I:inpCapacity", bufferCapacity); }
	@Override public void writeConfig(JsonWriter writer) throws IOException { writer.name("I:inpCapacity").value(bufferCapacity); }

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) bb = AxisAlignedBB.getBoundingBox(xCoord - 2, yCoord, zCoord - 2, xCoord + 3, yCoord + 5, zCoord + 3);
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
		return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
	}

	@Override
	public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
		info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.machine_moxer));
		if(type == UpgradeType.SPEED) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(KEY_SPEED, "+" + (level * 100 / 3) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey(KEY_CONSUMPTION, "+" + (level * 50) + "%"));
		}
		if(type == UpgradeType.POWER) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(KEY_CONSUMPTION, "-" + (level * 25) + "%"));
		}
		if(type == UpgradeType.OVERDRIVE) {
			IUpgradeInfoProvider.addOverdriveInfo(info, extendedInfo, 1 + Math.min(level, 3), 1 + Math.min(level, 3) * 10D / 3D);
			info.add((BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY) + I18nUtil.resolveKey("gui.common.yes_upper"));
		}
	}

	@Override
	public HashMap<UpgradeType, Integer> getValidUpgrades() {
		HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
		upgrades.put(UpgradeType.SPEED, 3);
		upgrades.put(UpgradeType.POWER, 3);
		upgrades.put(UpgradeType.OVERDRIVE, 3);
		return upgrades;
	}
}
