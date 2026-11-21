package com.hbm.tileentity.machine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMoxer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMoxer;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MoxerStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.MoxerRecipe;
import com.hbm.inventory.recipes.MoxerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.config.MachineConfig;
import com.hbm.items.machine.ICustomFuelItem;
import com.hbm.items.machine.ItemCustomFuel;
import com.hbm.items.machine.ItemCustomRBMKRod;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSources;
import com.hbm.items.machine.customfuel.CustomFuelSpent;
import com.hbm.items.machine.customfuel.EnumCustomFuel;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.main.NTMSounds;
import com.hbm.module.machine.ModuleMachineMoxer;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.*;
import com.hbm.util.BobMathUtil;

import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.fauxpointtwelve.DirPos;
import com.hbm.util.i18n.I18nUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMoxer extends TileEntityMachineBase implements IGUIProvider, IConfigurableMachine, IMetalCopiable, IControlReceiver, IEnergyReceiverMK2, IUpgradeInfoProvider, IFluidStandardReceiverMK2 {

	public static final String MOXER_POOL = GenericRecipes.POOL_PREFIX_ALT + "moxer";

	public List<MoxerStack> inpStack = new ArrayList();

	// CONFIGURABLE CONSTANTS
	//because eclipse's auto complete is dumb as a fucking rock, it's now called "ZCapacity" so it's listed AFTER the actual stacks in the auto complete list.
	//also martin i know you read these: no i will not switch to intellij after using eclipse for 8 years.
	public static int inpZCapacity = MaterialShapes.BLOCK.q(16);

	public FluidTank[] inputTanks;
	public long power;
	public long maxPower = 1_000_000;
	public double progress;
	public Status status = Status.IDLE;
	public boolean autoMode;
	private AudioWrapper audio;

	public ModuleMachineMoxer moxerModule;
	public UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
	public boolean frame = false;
	public int anim;
	public int prevAnim;

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public boolean canProvideInfo(ItemMachineUpgrade.UpgradeType type, int level, boolean extendedInfo) {
		return type == ItemMachineUpgrade.UpgradeType.SPEED || type == ItemMachineUpgrade.UpgradeType.POWER || type == ItemMachineUpgrade.UpgradeType.OVERDRIVE;
	}

	@Override
	public void provideInfo(ItemMachineUpgrade.UpgradeType type, int level, List<String> info, boolean extendedInfo) {
		info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.machine_moxer));
		if(type == ItemMachineUpgrade.UpgradeType.SPEED) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(KEY_SPEED, "+" + (level * 100 / 3) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey(KEY_CONSUMPTION, "+" + (level * 50) + "%"));
		}
		if(type == ItemMachineUpgrade.UpgradeType.POWER) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(KEY_CONSUMPTION, "-" + (level * 25) + "%"));
		}
		if(type == ItemMachineUpgrade.UpgradeType.OVERDRIVE) {
			info.add((BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY) + "YES");
		}
	}

	@Override
	public HashMap<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
		HashMap<ItemMachineUpgrade.UpgradeType, Integer> upgrades = new HashMap<>();
		upgrades.put(ItemMachineUpgrade.UpgradeType.SPEED, 3);
		upgrades.put(ItemMachineUpgrade.UpgradeType.POWER, 3);
		upgrades.put(ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3);
		return upgrades;
	}

	@Override
	public FluidTank[] getReceivingTanks() { return inputTanks; }

	@Override
	public FluidTank[] getAllTanks() { return inputTanks; }

	public enum Status {
		IDLE,
		READY,
		RUNNING
	}

	public String getStatus(Status state) {
		switch (state) {
			case IDLE:
				return "IDLE";
			case READY:
				return "READY";
			case RUNNING:
				return "RUNNING";
			default:
				return "ERROR";
		}
	}

	@Override
	public String getConfigName() {
		return "moxer";
	}

	@Override
	public void readIfPresent(JsonObject obj) {
		inpZCapacity = IConfigurableMachine.grab(obj, "I:inpCapacity", inpZCapacity);
	}

	@Override
	public void writeConfig(JsonWriter writer) throws IOException {
		writer.name("I:inpCapacity").value(inpZCapacity);
	}

	public TileEntityMoxer() {
		super(12);
		this.inputTanks = new FluidTank[3];
		// this process actually exists and it's called coprecipitation, it technically needs a whole lot of important chems but who cares
		for (int i = 0; i < inputTanks.length; i++)
			this.inputTanks[i] = new FluidTank(Fluids.NONE, 16_000);

		this.moxerModule = new ModuleMachineMoxer(0, this, slots)
			.itemOutput(5)
			.fluidInput(inputTanks[0], inputTanks[1], inputTanks[2]);
	}

	@Override
	public String getName() {
		return "container.machineMoxer";
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public void updateEntity() {

		if(maxPower <= 0) this.maxPower = 1_000_000;

		if (!worldObj.isRemote) {
			MoxerRecipe recipe = MoxerRecipes.INSTANCE.recipeNameMap.get(moxerModule.getRecipeName());
			if(recipe != null) {
				this.maxPower = recipe.power * 100;
			}
			this.maxPower = BobMathUtil.max(this.power, this.maxPower, 1_000_000);

			this.power = Library.chargeTEFromItems(slots, 0, power, maxPower);
			upgradeManager.checkSlots(slots, 2, 3);

			this.inputTanks[0].loadTank(6, 9, slots);
			this.inputTanks[1].loadTank(7, 10, slots);
			this.inputTanks[2].loadTank(8, 11, slots);

			this.setupAllPorts(getPorts());
			this.updatePortPIFIFO();

			double speed = 1D;
			double pow = 1D;

			speed += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED), 3) / 3D;
			speed += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.OVERDRIVE), 3);

			pow -= Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.POWER), 3) * 0.25D;
			pow += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED), 3) * 1D;
			pow += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.OVERDRIVE), 3) * 10D / 3D;

			int totalOut = 0;

			for(MoxerStack stack : inpStack) {
				totalOut += stack.percentage;
			}

			//this.moxerModule.update(speed, pow, true, slots[1]);
			if (recipe != null)
				this.moxerModule.setupTanks(recipe);

			// smelt items from buffer
			trySmelt();

			// clean up stacks
			this.inpStack.removeIf(x -> x.amount <= 0);

			ItemStack product = recipe != null ? buildProduct(recipe) : null;
			long consumption = recipe == null ? 0 : (pow == 1 ? recipe.power : (long) (recipe.power * pow));

			status = Status.IDLE;

			if (product != null && totalOut >= recipe.amount && power >= consumption && hasFluids(recipe) && canFitOutput(product)) {
				boolean enoughMaterial = true;
				for (MoxerStack stack : inpStack) {
					if (stack.amount < stack.percentage) {
						enoughMaterial = false;
						break;
					}
				}
				if (enoughMaterial) status = autoMode ? Status.RUNNING : Status.READY;
			}

			// activated
			if (status == Status.RUNNING) {
				progress += Math.min(speed / recipe.duration, 1D);
				power -= consumption;
			} else {
				progress = 0.0D;
			}

			// finished
			if (progress >= 1.0D) {
				progress = 0.0D;
				for (MoxerStack stack : inpStack) {
					if (stack.percentage > 0) stack.amount -= stack.percentage;
				}
				if (recipe.inputFluid != null) {
					for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
						inputTanks[i].setFill(inputTanks[i].getFill() - recipe.inputFluid[i].fill);
					}
				}
				if (slots[5] == null) {
					slots[5] = product;
				} else {
					slots[5].stackSize++;
				}
			}

			this.markDirty();

			// sync
			this.networkPackNT(25);
		} else {
			this.prevAnim = this.anim;
			boolean didSomething = progress > 0;
			if(didSomething) this.anim++;

			//steam out of the fan
			if(didSomething && worldObj.rand.nextInt(10) == 0) {
				worldObj.spawnParticle("cloud", xCoord + 0.1D + worldObj.rand.nextDouble() * 0.8D, yCoord + 2.5D, zCoord + 0.1D + worldObj.rand.nextDouble() * 0.8D, 0.0, 0.1, 0.0);
			}

			if(worldObj.getTotalWorldTime() % 20 == 0) {
				frame = !worldObj.getBlock(xCoord, yCoord + 3, zCoord).isAir(worldObj, xCoord, yCoord + 3, zCoord);
			}

			if (didSomething && MainRegistry.proxy.me().getDistance(xCoord , yCoord, zCoord) < 50) {
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

	// The item one finished cycle would produce with the current mix, or null if the recipe has no output
	protected ItemStack buildProduct(MoxerRecipe recipe) {
		if (recipe.outputItem == null || recipe.outputItem.length == 0) return null;
		ItemStack out = recipe.outputItem[0].collapse();
		if (out == null) return null;
		out = out.copy();
		out.stackSize = 1;

		if (out.getItem() instanceof ICustomFuelItem) {
			List<Mats.MaterialStack> mix = new ArrayList();
			for (MoxerStack stack : inpStack) {
				if (stack.percentage > 0) mix.add(new Mats.MaterialStack(stack.material, stack.percentage));
			}
			ItemCustomFuel.setup(out, mix, recipe.amount);
			if (out.getItem() instanceof ItemCustomRBMKRod) ItemCustomRBMKRod.prepare(out);
		}
		return out;
	}

	protected boolean hasFluids(MoxerRecipe recipe) {
		if (recipe.inputFluid == null) return true;
		for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
			if (inputTanks[i].getFill() < recipe.inputFluid[i].fill) return false;
		}
		return true;
	}

	protected boolean canFitOutput(ItemStack product) {
		ItemStack current = slots[5];
		if (current == null) return true;
		return current.getItem() == product.getItem()
			&& current.getItemDamage() == product.getItemDamage()
			&& ItemStack.areItemStackTagsEqual(current, product)
			&& current.stackSize < current.getMaxStackSize();
	}

	@Override public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound(NTMSounds.CENTRIFUGE_LOOP, xCoord, yCoord, zCoord, 1F, 15F, 1.0F, 20);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);

		buf.writeBoolean(autoMode);
		buf.writeByte(status.ordinal());
		buf.writeDouble(progress);
		buf.writeLong(power);
		buf.writeLong(maxPower);
		for(FluidTank tank : inputTanks) tank.serialize(buf);
		this.moxerModule.serialize(buf);

		buf.writeShort(inpStack.size());
		for(MoxerStack sta : inpStack) {
			if (sta.material == null)
				buf.writeInt(-1);
			else
				buf.writeInt(sta.material.id);
			buf.writeInt(sta.amount);
			buf.writeInt(sta.percentage);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);

		this.autoMode = buf.readBoolean();
		this.status = Status.values()[buf.readByte()];
		this.progress = buf.readDouble();
		this.power = buf.readLong();
		this.maxPower = buf.readLong();
		for(FluidTank tank : inputTanks) tank.deserialize(buf);
		this.moxerModule.deserialize(buf);

		inpStack.clear();

		int mats = buf.readShort();
		for(int i = 0; i < mats; i++) {
			int id = buf.readInt();
			int amount = buf.readInt();
			int percentage = buf.readInt();
			NTMMaterial mat = Mats.matById.get(id);
			if (mat == null) continue;
			inpStack.add(new MoxerStack(mat, amount, percentage));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		autoMode = nbt.getBoolean("autoMode");
		power = nbt.getLong("power");
		maxPower = nbt.getLong("maxPower");
		this.moxerModule.readFromNBT(nbt);
		int[] input = nbt.getIntArray("input");
		for(int i = 0; i < input.length / 3; i++) {
			NTMMaterial mat = Mats.matById.get(input[i * 3]);
			if(mat == null) continue;
			inpStack.add(new MoxerStack(mat, input[i * 3 + 1], input[i * 3 + 2]));
		}
		for(int i = 0; i < inputTanks.length; i++) inputTanks[i].readFromNBT(nbt, "t" + i);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setBoolean("autoMode", autoMode);
		nbt.setLong("power", power);
		nbt.setLong("maxPower", maxPower);
		this.moxerModule.writeToNBT(nbt);
		int[] input = new int[inpStack.size() * 3];
		for(int i = 0; i < inpStack.size(); i++) {
			MoxerStack sta = inpStack.get(i);
			input[i * 3] = sta.material.id;
			input[i * 3 + 1] = sta.amount;
			input[i * 3 + 2] = sta.percentage;
		}
		nbt.setIntArray("input", input);
		for(int i = 0; i < inputTanks.length; i++) inputTanks[i].writeToNBT(nbt, "t" + i);
	}

	protected PortDef[] cachedPorts;
	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			DirPos[] cons = getConPos();
			cachedPorts = new PortDef[cons.length];
			for(int i = 0; i < cons.length; i++) {
				ForgeDirection dir = cons[i].getDir();
				cachedPorts[i] = PortDef.make(cons[i].getX() - dir.offsetX, cons[i].getY(), cons[i].getZ() - dir.offsetZ, dir);
			}
		}
		return cachedPorts;
	}

	public DirPos[] getConPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		return new DirPos[] {
			new DirPos(xCoord + dir.offsetX * 2 + rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 + rot.offsetZ, dir),
			new DirPos(xCoord + dir.offsetX * 2 - rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 - rot.offsetZ, dir),
			new DirPos(xCoord - dir.offsetX * 2 + rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
			new DirPos(xCoord - dir.offsetX * 2 - rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
			new DirPos(xCoord + rot.offsetX * 3, yCoord, zCoord + rot.offsetZ * 3, rot),
			new DirPos(xCoord - rot.offsetX * 3, yCoord, zCoord - rot.offsetZ * 3, rot.getOpposite())
		};
	}

	protected boolean trySmelt() {

		ItemStack slot = slots[4];
		if (slot == null || !isItemSmeltable(slot)) return false;

		for (MoxerStack material : getFuelMaterials(slot))
			this.addToStack(this.inpStack, material);

		this.decrStackSize(4, 1);

		return true;
	}

	public static boolean isFuelMaterial(NTMMaterial mat) {
		return EnumCustomFuel.byMaterial(mat) != null;
	}

	// What is left of a custom fuel item right now, null if it can't go into the Moxer
	public static CustomFuelMix getRecyclableMix(ItemStack stack) {
		Item item = stack.getItem();
		if (!(item instanceof ICustomFuelItem) || item == ModItems.custom_fuel_pwr_hot) return null;
		if (CustomFuelSpent.isSpent(stack) && !MachineConfig.customFuelRecycles()) return null;

		if (item instanceof ItemCustomRBMKRod && (ItemRBMKRod.getHullHeat(stack) >= 50 || ItemRBMKRod.getCoreHeat(stack) >= 50)) return null;
		return ICustomFuelItem.currentMix(stack);
	}

	// The fuel isotopes dissolving this item would add to the buffer; anything else in it is discarded
	public static List<MoxerStack> getFuelMaterials(ItemStack stack) {
		List<MoxerStack> fuels = new ArrayList();
		if (stack == null) return fuels;

		if (stack.getItem() instanceof ICustomFuelItem) {
			CustomFuelMix mix = getRecyclableMix(stack);
			if (mix == null) return fuels;
			for (EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if (mix.get(fuel) > 0) fuels.add(new MoxerStack(fuel.stack, mix.get(fuel), 0));
			}
			return fuels;
		}

		CustomFuelMix mix = CustomFuelSources.dissolve(stack);
		if (mix == null) return fuels;
		for (EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if (mix.get(fuel) > 0) fuels.add(new MoxerStack(fuel.stack, mix.get(fuel), 0));
		}
		return fuels;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if (i == 0) return true; // battery
		if (i == 1 && stack.getItem() == ModItems.blueprints) return MOXER_POOL.equals(ItemBlueprints.grabPool(stack));
		if (i >= 2 && i <= 3 && stack.getItem() instanceof ItemMachineUpgrade) return true; // upgrades
		if (i == 4) { return isItemSmeltable(stack); }
		if (i >= 6 && i <= 8) return true; // input fluid
		return false;
	}

	public boolean isItemSmeltable(ItemStack stack) {

		List<MoxerStack> materials = getFuelMaterials(stack);
		if (materials.isEmpty()) return false;

		int inpAmount = getQuantaFromType(this.inpStack, null);
		for (MoxerStack mat : materials) inpAmount += mat.amount;

		return inpAmount <= inpZCapacity;
	}

	public void addToStack(List<MoxerStack> stack, MoxerStack matStack) {

		for(MoxerStack mat : stack) {
			if(mat.material == matStack.material) {
				mat.amount += matStack.amount;
				return;
			}
		}

		stack.add(matStack.copy());
	}

	public int getQuantaFromType(List<MoxerStack> stacks, NTMMaterial mat) {
		int sum = 0;
		for(MoxerStack stack : stacks) {
			if(stack.material == mat) {
				return stack.amount;
			}
			if(mat == null) {
				sum += stack.amount;
			}
		}
		return sum;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int meta) {
		return new int[] { 4, 5, 6, 7, 8, 9, 10, 11 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 5 || (slot >= 9 && slot <= 11);
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMoxer(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMoxer(player.inventory, this);
	}

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
	public int[] getMatsToCopy() {
		ArrayList<Integer> types = new ArrayList<>();

		for (MoxerStack stack : inpStack) {
			types.add(stack.material.id);
		}
		return BobMathUtil.intCollectionToArray(types);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(EntityPlayer player, NBTTagCompound data) {
		int percIndex = data.getInteger("perc_index");
		if(data.hasKey("perc_index") && data.hasKey("percentage") && percIndex >= 0 && percIndex < inpStack.size()) {
			int index = percIndex;
			int percentage = Math.max(data.getInteger("percentage"), 0);
			MoxerStack ref = inpStack.get(index);
			int tot_perc = 0;
			for (MoxerStack mat : inpStack)
				if (mat != ref)
					tot_perc += mat.percentage;
			if (moxerModule.getRecipeName() != null) {
				MoxerRecipe recipe = MoxerRecipes.INSTANCE.recipeNameMap.get(moxerModule.getRecipeName());
				if (recipe != null && tot_perc + percentage > recipe.amount)
					percentage = recipe.amount - tot_perc;
				inpStack.get(index).percentage = percentage;
			}
		}
		if (data.hasKey("autoChange"))
			autoMode = !autoMode;
		// recipe
		if(data.hasKey("index") && data.hasKey("selection")) {
			int index = data.getInteger("index");
			String selection = data.getString("selection");
			if(index == 0) {
				this.moxerModule.setRecipe(selection, false);
				this.markChanged();
			}
			for (MoxerStack mat : inpStack)
				mat.percentage = 0;
		}
	}

	public List<String> getStringMaterials() {

		if(!inpStack.isEmpty()) {
			List<String> array = new ArrayList();

			for (MoxerStack mat : inpStack) {
                array.add(mat.material.getUnlocalizedName());
            }

			if(array.isEmpty())
				return null;

			return array;
		}

		return null;
	}
}
