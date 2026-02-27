package com.scarasol.rummage.data;

import com.scarasol.rummage.api.mixin.IRummageableEntity;

public record RummageTarget(IRummageableEntity entity, int localSlotIndex) {}
