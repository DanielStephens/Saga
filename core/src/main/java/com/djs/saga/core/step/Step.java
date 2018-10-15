package com.djs.saga.core.step;

public interface Step<STEP_INPUT> {

	StepOutput run(StepParams stepParams, STEP_INPUT stepInput);

}
