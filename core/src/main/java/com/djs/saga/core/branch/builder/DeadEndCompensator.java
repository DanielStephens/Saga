package com.djs.saga.core.branch.builder;

@FunctionalInterface
public interface DeadEndCompensator<AWAITED_VALUE> {

	Waiter<DeadEnd> instrument(Waiter<AWAITED_VALUE> waiter);

}
