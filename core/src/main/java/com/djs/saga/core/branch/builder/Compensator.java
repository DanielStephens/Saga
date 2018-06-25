package com.djs.saga.core.branch.builder;

@FunctionalInterface
public interface Compensator<AWAITED_VALUE> {

	Waiter<AWAITED_VALUE> instrument(Waiter<AWAITED_VALUE> waiter);

}
