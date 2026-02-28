package com.scarasol.rummage.api.mixin;


/**
 * @author Scarasol
 */
public interface ILootrInventoryDelegate {
    void rummage$setBlockEntity(IRummageableEntity entity);
    IRummageableEntity rummage$getBlockEntity();
}