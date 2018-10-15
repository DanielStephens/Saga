package com.djs.saga.core.saga;

import java.util.UUID;

import com.djs.saga.core.saga.lifecycle.SagaEvents;

/**
 * Remember to register the {@link Saga} with {@link com.djs.saga.core.register.Sagas} manually when building.
 *
 * @param <SAGA_INPUT>
 */
public interface Saga<SAGA_INPUT> {

	default SagaOutput<SAGA_INPUT> run(SAGA_INPUT sagaInput){
		return run(UUID.randomUUID(), sagaInput);
	}

	SagaOutput<SAGA_INPUT> run(UUID correlationId, SAGA_INPUT sagaInput);

	void send(SagaEvents.SagaEvent sagaEvent);

	SagaId getSagaId();

}
