package com.hbm.packet.toserver;

import java.io.IOException;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hbm.interfaces.IControlReceiver;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class NBTControlPacket implements IMessage {

	PacketBuffer buffer;
	int x;
	int y;
	int z;

	public NBTControlPacket() { }

	public NBTControlPacket(NBTTagCompound nbt, int x, int y, int z) {

		this.buffer = new PacketBuffer(Unpooled.buffer());
		this.x = x;
		this.y = y;
		this.z = z;

		try {
			buffer.writeNBTTagCompoundToBuffer(nbt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {

		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();

		if(buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

		buffer.writeBytes(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {

		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);

		if (buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

		buf.writeBytes(buffer);
	}

	public static class Handler implements IMessageHandler<NBTControlPacket, IMessage> {

		@Override
		public IMessage onMessage(NBTControlPacket m, MessageContext ctx) {

			EntityPlayer p = ctx.getServerHandler().playerEntity;
			World world = p.worldObj;

			try {

				NBTTagCompound nbt = m.buffer.readNBTTagCompoundFromBuffer();

				if(nbt != null && world != null) {
					int x = m.x;
					int y = m.y;
					int z = m.z;
					ServerThreadUtil.addScheduledTask(() -> {
						if(p.isDead || p.worldObj != world || !world.blockExists(x, y, z)) return;
						TileEntity te = world.getTileEntity(x, y, z);
						if(!(te instanceof IControlReceiver) || te.isInvalid()) return;
						IControlReceiver tile = (IControlReceiver) te;
						if(!tile.hasPermission(p)) return;

						PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(p, Action.RIGHT_CLICK_BLOCK, x, y, z, 1, world);
						if(!event.isCanceled() && p.worldObj == world && !te.isInvalid()
								&& world.getTileEntity(x, y, z) == te && tile.hasPermission(p)) {
							tile.receiveControl(p, nbt);
						}
					});
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				m.buffer.release();
			}

			return null;
		}
	}
}
