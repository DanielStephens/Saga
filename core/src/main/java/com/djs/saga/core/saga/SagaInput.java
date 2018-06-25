package com.djs.saga.core.saga;

import java.util.UUID;

import lombok.Value;

@Value
public class SagaInput<SAGA_INPUT> {

	Saga<SAGA_INPUT> saga;
	UUID correlationId;
	SAGA_INPUT sagaInput;

}
