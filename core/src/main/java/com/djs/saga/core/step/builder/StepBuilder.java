package com.djs.saga.core.step.builder;

import java.util.concurrent.ExecutorService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StepBuilder {

	private final ExecutorService executorService;

	public StepBuilderAction start(String name) {
		return new StepBuilderAction(
				executorService,
				name
		);
	}

}
