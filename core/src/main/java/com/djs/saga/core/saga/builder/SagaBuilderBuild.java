package com.djs.saga.core.saga.builder;

import com.djs.saga.core.register.Sagas;
import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.SagaId;
import com.djs.saga.core.saga.SagaImpl;
import com.djs.saga.core.saga.lifecycle.SagaEvents;
import com.djs.saga.core.saga.lifecycle.SagaLifecycleHandler;
import com.djs.saga.core.step.Step;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SagaBuilderBuild<SAGA_INPUT> {

	private final Sagas sagas;
	private final SagaId sagaId;
	private final SagaLifecycleHandler<SAGA_INPUT> lifecycleHandler;
	private final Step<SAGA_INPUT> step;

	public Saga<SAGA_INPUT> build() {
		SagaImpl<SAGA_INPUT> saga = new SagaImpl<>(
				sagaId,
				lifecycleHandler,
				step
		);
		sagas.register(saga);
		saga.send(SagaEvents.ON_BUILD_EVENT);
		return saga;
	}

}
