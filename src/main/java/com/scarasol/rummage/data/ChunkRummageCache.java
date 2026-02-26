package com.scarasol.rummage.data;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

/**
 * 封装一个区块内所有被搜刮过的容器/实体的进度。
 *
 * @author Scarasol
 */
public record ChunkRummageCache(Map<UUID, Map<UUID, BitSet>> progresses) {
}