package com.djs.saga.core.step.builder;

import java.util.concurrent.ExecutorService;

import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.StepImpl;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StepBuilderBuild<STEP_INPUT> {

	private final ExecutorService executorService;
	private final String name;
	private final Action<STEP_INPUT> action;
	private final BranchesBuilder<STEP_INPUT> branchesBuilder;

	public Step<STEP_INPUT> build() {
		return new StepImpl<>(
				executorService,
				name,
				action,
				branchesBuilder
		);
	}
}
