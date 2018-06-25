package com.djs.saga.core.step.builder;

import java.util.concurrent.ExecutorService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StepBuilderAction {

	private final ExecutorService executorService;
	private final String name;

	public <STEP_INPUT> StepBuilderBranches<STEP_INPUT> withAction(Action<STEP_INPUT> action) {
		return new StepBuilderBranches<>(
				executorService,
				name,
				action
		);
	}

}
