package com.djs.saga.core.saga.builder;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionOperations;

import com.djs.saga.core.persistence.SagaInputRepository;
import com.djs.saga.core.register.Sagas;
import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.SagaId;
import com.djs.saga.core.saga.SagaInput;
import com.djs.saga.core.saga.lifecycle.SagaEvents;
import com.djs.saga.core.saga.lifecycle.SagaLifecycleHandler;
import com.djs.saga.core.step.Step;

public class SagaBuilderAdditions<SAGA_INPUT> extends SagaBuilderBuild<SAGA_INPUT> {

	private final Sagas sagas;
	private final Optional<TransactionOperations> transactionOperations;
	private final SagaId sagaId;
	private final SagaLifecycleHandler<SAGA_INPUT> lifecycleHandler;
	private final Step<SAGA_INPUT> step;

	public SagaBuilderAdditions(Sagas sagas,
								Optional<TransactionOperations> transactionOperations,
								SagaId sagaId,
								SagaLifecycleHandler<SAGA_INPUT> lifecycleHandler,
								Step<SAGA_INPUT> step) {
		super(sagas, sagaId, lifecycleHandler, step);
		this.sagas = sagas;
		this.transactionOperations = transactionOperations;
		this.sagaId = sagaId;
		this.lifecycleHandler = lifecycleHandler;
		this.step = step;
	}

	public SagaBuilderAdditions<SAGA_INPUT> beforeStepDo(Consumer<SagaInput<SAGA_INPUT>> b) {
		return new SagaBuilderAdditions<>(
				sagas,
				transactionOperations,
				sagaId,
				lifecycleHandler.then(new SagaLifecycleHandler<SAGA_INPUT>() {
					@Override
					public void before(SagaInput<SAGA_INPUT> input) {
						b.accept(input);
					}
				}),
				step
		);
	}

	public SagaBuilderAdditions<SAGA_INPUT> afterStepDo(Consumer<SagaInput<SAGA_INPUT>> a) {
		return new SagaBuilderAdditions<>(
				sagas,
				transactionOperations,
				sagaId,
				lifecycleHandler.then(new SagaLifecycleHandler<SAGA_INPUT>() {
					@Override
					public void after(SagaInput<SAGA_INPUT> input) {
						a.accept(input);
					}
				}),
				step
		);
	}

	public SagaBuilderAdditions<SAGA_INPUT> withRecovery(SagaInputRepository<SAGA_INPUT> sagaInputRepository, Consumer<Collection<SagaInput<SAGA_INPUT>>> recoveryAction) {
		TransactionOperations transactionOperations = this.transactionOperations
				.orElseThrow(() -> new UnsupportedOperationException("No bean of type TransactionOperations was present so no persistence functionality can be used."));

		return new SagaBuilderAdditions<>(
				sagas,
				this.transactionOperations,
				sagaId,
				lifecycleHandler.then(new SagaLifecycleHandler<SAGA_INPUT>() {
					@Override
					public void before(SagaInput<SAGA_INPUT> input) {
						transactionOperations.execute(s -> {
							sagaInputRepository.persist(input);
							return true;
						});
					}

					@Override
					public void after(SagaInput<SAGA_INPUT> input) {
						transactionOperations.execute(s -> {
							sagaInputRepository.remove(input);
							return true;
						});
					}

					@Override
					public void onEvent(Saga<SAGA_INPUT> saga, SagaEvents.SagaEvent sagaEvent) {
						if (!sagaEvent.asStartRecoveryEvent().isPresent()) {
							return;
						}

						transactionOperations.execute(s -> {
							recoveryAction.accept(sagaInputRepository.recover(saga.getSagaId()));
							return true;
						});
					}
				}),
				step
		);
	}

}
