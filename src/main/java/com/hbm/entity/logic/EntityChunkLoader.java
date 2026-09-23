package com.hbm.entity.logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

/** Keeps an entity ticket synchronized with the chunks the entity currently needs. */
public class EntityChunkLoader {

	private final Entity entity;
	private Ticket ticket;
	private final Set<ChunkCoordIntPair> forcedChunks = new HashSet<ChunkCoordIntPair>();

	public EntityChunkLoader(Entity entity) {
		this.entity = entity;
	}

	public void init(Ticket ticket) {
		if(entity.worldObj.isRemote || ticket == null) return;

		if(this.ticket != null && this.ticket != ticket) {
			ForgeChunkManager.releaseTicket(this.ticket);
		}

		this.ticket = ticket;
		this.ticket.bindEntity(entity);
		this.ticket.getModData();

		forcedChunks.clear();
		forcedChunks.addAll(ticket.getChunkList());
		loadChunk(entity.chunkCoordX, entity.chunkCoordZ);
	}

	public void loadChunk(int chunkX, int chunkZ) {
		if(entity.worldObj.isRemote || ticket == null) return;

		Set<ChunkCoordIntPair> desiredChunks = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
		if(forcedChunks.equals(desiredChunks)) return;

		for(ChunkCoordIntPair chunk : desiredChunks) {
			if(!forcedChunks.contains(chunk)) {
				ForgeChunkManager.forceChunk(ticket, chunk);
			}
		}

		for(ChunkCoordIntPair chunk : forcedChunks) {
			if(!desiredChunks.contains(chunk)) {
				ForgeChunkManager.unforceChunk(ticket, chunk);
			}
		}

		forcedChunks.clear();
		forcedChunks.addAll(desiredChunks);
	}

	public void clear() {
		if(!entity.worldObj.isRemote && ticket != null) {
			ForgeChunkManager.releaseTicket(ticket);
			ticket = null;
		}
		forcedChunks.clear();
	}
}
