package com.djs.saga.core.step.builder;

import java.util.UUID;

@FunctionalInterface
public interface Action<STEP_INPUT> {

	void perform(UUID correlationId, STEP_INPUT stepInput);

}
