package com.djs.saga.core.step;

import java.util.UUID;

import com.djs.saga.core.display.SagaToStringBuilder;

import lombok.Value;

@Value
public class StepParams {

	SagaToStringBuilder toStringBuilder;
	UUID sagaId;

}
