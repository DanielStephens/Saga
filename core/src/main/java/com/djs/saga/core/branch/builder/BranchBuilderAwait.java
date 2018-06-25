package com.djs.saga.core.branch.builder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BranchBuilderAwait {

	private final String name;

	public <AWAITED_VALUE> BranchBuilderThen<AWAITED_VALUE> await(Waiter<AWAITED_VALUE> waiter) {
		return new BranchBuilderThen<>(
				name,
				waiter
		);
	}

}
