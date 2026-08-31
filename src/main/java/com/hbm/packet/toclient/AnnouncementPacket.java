package com.hbm.packet.toclient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.audio.PositionedSoundRecord;

public class AnnouncementPacket implements IMessage {

	private String sound;
	private float volume;
	private float pitch;

	public AnnouncementPacket() {
		this.sound = "hbm:alarm.announcement";
		this.volume = 1.0F;
		this.pitch = 1.0F;
	}

	public AnnouncementPacket(String sound, float volume, float pitch) {
		this.sound = sound;
		this.volume = volume;
		this.pitch = pitch;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int len = buf.readInt();
		byte[] bytes = new byte[len];
		buf.readBytes(bytes);
		this.sound = new String(bytes);
		this.volume = buf.readFloat();
		this.pitch = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		byte[] bytes = this.sound.getBytes();
		buf.writeInt(bytes.length);
		buf.writeBytes(bytes);
		buf.writeFloat(this.volume);
		buf.writeFloat(this.pitch);
	}

	public static class Handler implements IMessageHandler<AnnouncementPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(final AnnouncementPacket m, MessageContext ctx) {
			final Minecraft mc = Minecraft.getMinecraft();
			if(mc != null) {
				mc.func_152344_a(new Runnable() {
					@Override
					public void run() {
						if(mc.getSoundHandler() != null) {
							mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation(m.sound), m.pitch));
						}
					}
				});
			}
			return null;
		}
	}
}
