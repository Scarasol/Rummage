package com.scarasol.rummage.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 *
 * @author Scarasol
 */
public record GlobalChunkPos(ResourceKey<Level> dimension, ChunkPos chunkPos) {
}