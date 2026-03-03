package com.scarasol.rummage.api.mixin;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

/**
 * @author Scarasol
 */
public interface ICorpseRummageable {
    Map<UUID, BitSet> rummage$getProgress(int type);
}