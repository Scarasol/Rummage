package com.scarasol.rummage.data;

import com.scarasol.rummage.api.mixin.IRummageable;

public record RummageTarget(IRummageable entity, int localSlotIndex) {}
