package com.scarasol.rummage.api.mixin;


/**
 * @author Scarasol
 */
public interface ILootrInventoryDelegate {
    void rummage$setBlockEntity(IRummageable entity);
    IRummageable rummage$getBlockEntity();
}