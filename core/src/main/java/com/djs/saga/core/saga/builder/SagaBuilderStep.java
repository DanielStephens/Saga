package com.djs.saga.core.saga.builder;

import java.util.Optional;

import org.springframework.transaction.support.TransactionOperations;

import com.djs.saga.core.register.Sagas;
import com.djs.saga.core.saga.SagaId;
import com.djs.saga.core.saga.lifecycle.NoopSagaLifecycleHandler;
import com.djs.saga.core.step.Step;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SagaBuilderStep {

	private final Sagas sagas;
	private final Optional<TransactionOperations> transactionOperations;
	private final SagaId sagaId;

	public <SAGA_INPUT> SagaBuilderAdditions<SAGA_INPUT> withStep(Step<SAGA_INPUT> step) {
		return new SagaBuilderAdditions<>(
				sagas,
				transactionOperations,
				sagaId,
				new NoopSagaLifecycleHandler<>(),
				step
		);
	}

}
