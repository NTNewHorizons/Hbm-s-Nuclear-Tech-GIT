package com.hbm.commands;

import com.hbm.config.GeneralConfig;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.main.MainRegistry;
import com.hbm.util.BobMathUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.TimeUnit;

import static com.hbm.handler.threading.PacketThreading.totalCnt;
import net.minecraft.util.ChatComponentTranslation;

public class CommandPacketInfo extends CommandBase {
	@Override
	public String getCommandName() {
		return "ntmpackets";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return EnumChatFormatting.RED + "/ntmpackets [info/resetState/toggleThreadingStatus/forceLock/forceUnlock]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {

		if (args.length > 0) {
			switch (args[0]) {
				case "resetState":
					PacketThreading.hasTriggered = false;
					PacketThreading.clearCnt = 0;
					return;
				case "toggleThreadingStatus":
					GeneralConfig.enablePacketThreading = !GeneralConfig.enablePacketThreading; // Force toggle.
					PacketThreading.init(); // Reinit threads.
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_sending_status_toggled_to_s", EnumChatFormatting.GREEN, GeneralConfig.enablePacketThreading));
					return;
				case "forceLock":
					PacketThreading.lock.lock(); // oh my fucking god never do this please unless you really have to
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_thread_lock_acquired_this_may_freeze_the_main_t", EnumChatFormatting.RED));
					MainRegistry.logger.error("Packet thread lock acquired by {}, this may freeze the main thread!", sender.getCommandSenderName());
					return;
				case "forceUnlock":
					PacketThreading.lock.unlock();
					MainRegistry.logger.warn("Packet thread lock released by {}.", sender.getCommandSenderName());
					return;
				case "info":
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_ntm_packet_debugger_v1_2", EnumChatFormatting.GOLD));

					if (PacketThreading.isTriggered() && GeneralConfig.enablePacketThreading)
						sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_threading_errored_check_log", EnumChatFormatting.RED));
					else if (GeneralConfig.enablePacketThreading)
						sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_threading_active", EnumChatFormatting.GREEN));
					else
						sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_threading_inactive", EnumChatFormatting.RED));

					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_thread_pool_info", EnumChatFormatting.YELLOW));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_threads_total_s", EnumChatFormatting.YELLOW, PacketThreading.threadPool.getPoolSize()));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_threads_core_s", EnumChatFormatting.YELLOW, PacketThreading.threadPool.getCorePoolSize()));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_threads_idle_s", EnumChatFormatting.YELLOW, (PacketThreading.threadPool.getPoolSize() - PacketThreading.threadPool.getActiveCount())));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_threads_maximum_s", EnumChatFormatting.YELLOW, PacketThreading.threadPool.getMaximumPoolSize()));

					for (ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(false, false))
						if (thread.getThreadName().startsWith(PacketThreading.threadPrefix)) {
							sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_thread_name_s", EnumChatFormatting.GOLD, thread.getThreadName()));
							sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_thread_id_s", EnumChatFormatting.YELLOW, thread.getThreadId()));
							sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_thread_state_s", EnumChatFormatting.YELLOW, thread.getThreadState()));
							sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_locked_by_s", EnumChatFormatting.YELLOW, (thread.getLockOwnerName() == null ? "None" : thread.getLockName())));
						}

					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_packet_info", EnumChatFormatting.GOLD));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_amount_total_s", EnumChatFormatting.YELLOW, totalCnt));
					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_amount_remaining_s", EnumChatFormatting.YELLOW, PacketThreading.threadPool.getQueue().size()));

					if (totalCnt != 0)
						sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_remaining_to_process_s", EnumChatFormatting.YELLOW, BobMathUtil.roundDecimal(((double) PacketThreading.threadPool.getQueue().size() / totalCnt) * 100, 2)));

					sender.addChatMessage(new ChatComponentTranslation("chat.command_packet_info.s_time_spent_waiting_on_thread_s_last_tick_sms", EnumChatFormatting.YELLOW, BobMathUtil.roundDecimal(TimeUnit.MILLISECONDS.convert(PacketThreading.nanoTimeWaited, TimeUnit.NANOSECONDS), 4)));
					return;
			}
		}
		sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
	}
}
