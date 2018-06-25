package com.djs.saga.core.persistence;

import java.util.Collection;

import com.djs.saga.core.saga.SagaId;
import com.djs.saga.core.saga.SagaInput;

public interface SagaInputRepository<SAGA_INPUT> {

	/**
	 * Persist the {@link SagaInput} so that on a crash the input can be recovered. This may be called multiple times
	 * with the same input (for example when rerunning a saga after recovery), the implementation should be able to
	 * handle this case by either doing nothing or performing an update if desired.
	 * @param input
	 */
	void persist(SagaInput<SAGA_INPUT> input);

	/**
	 * Removes the persisted {@link SagaInput} as it
	 * @param input
	 */
	void remove(SagaInput<SAGA_INPUT> input);

	Collection<SagaInput<SAGA_INPUT>> recover(SagaId sagaId);

}
