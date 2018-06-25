package com.djs.saga.core.branch.builder;

import com.djs.saga.core.branch.Branch;
import com.djs.saga.core.branch.BranchImpl;
import com.djs.saga.core.step.Step;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BranchBuilderBuild<AWAITED_VALUE> {

	private final String name;
	private final Waiter<AWAITED_VALUE> waiter;
	private final Step<AWAITED_VALUE> step;

	public Branch build() {
		return new BranchImpl<>(
				name,
				waiter,
				step
		);
	}

}
