package com.djs.saga.core.step;

import java.util.UUID;

public interface Step<STEP_INPUT> {

	StepOutput run(String parentName, UUID correlationId, STEP_INPUT stepInput);

}
