package com.djs.saga.core.step.builder;

import java.util.concurrent.ExecutorService;

import com.djs.saga.core.branches.BranchesImpl;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StepBuilderBranches<STEP_INPUT> {

	private final ExecutorService executorService;
	private final String name;
	private final Action<STEP_INPUT> action;

	private StepBuilderBuild<STEP_INPUT> withBranches(BranchesBuilder<STEP_INPUT> branchesBuilder) {
		return new StepBuilderBuild<>(
				executorService,
				name,
				action,
				branchesBuilder
		);
	}

	public StepBuilderBuild<STEP_INPUT> withBranches(BranchCollectionBuilder<STEP_INPUT> branchCollectionBuilder) {
		BranchesBuilder<STEP_INPUT> f = (id, i, a) -> new BranchesImpl(branchCollectionBuilder.build(id, i, a));
		return withBranches(f);
	}

}
