package com.djs.saga.core.saga;

import com.djs.saga.core.saga.lifecycle.SagaEvents;

/**
 * Remember to register the {@link Saga} with {@link com.djs.saga.core.register.Sagas} manually when building.
 *
 * @param <SAGA_INPUT>
 */
public interface Saga<SAGA_INPUT> {

	SagaOutput<SAGA_INPUT> run(SAGA_INPUT sagaInput);

	void send(SagaEvents.SagaEvent sagaEvent);

	SagaId getSagaId();

}
