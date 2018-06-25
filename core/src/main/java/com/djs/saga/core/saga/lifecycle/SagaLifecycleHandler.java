package com.djs.saga.core.saga.lifecycle;

import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.SagaInput;

public interface SagaLifecycleHandler<SAGA_INPUT> {

	/**
	 * Called before the input is run through the {@link Saga}, for example to persist the input.
	 */
	default void before(SagaInput<SAGA_INPUT> input) {
	}

	/**
	 * Called after the {@link Saga} finishes running an input successfully.
	 *
	 * @param input
	 */
	default void after(SagaInput<SAGA_INPUT> input) {
	}

	/**
	 * Called when an arbitrary {@link SagaEvents.SagaEvent} is sent to a {@link Saga}.
	 */
	default void onEvent(Saga<SAGA_INPUT> saga, SagaEvents.SagaEvent sagaEvent) {
	}

	default SagaLifecycleHandler<SAGA_INPUT> then(SagaLifecycleHandler<SAGA_INPUT> then) {
		return new SagaLifecycleHandler<SAGA_INPUT>() {
			@Override
			public void before(SagaInput<SAGA_INPUT> input) {
				this.before(input);
				then.before(input);
			}

			@Override
			public void after(SagaInput<SAGA_INPUT> input) {
				this.after(input);
				then.after(input);
			}

			@Override
			public void onEvent(Saga<SAGA_INPUT> saga, SagaEvents.SagaEvent sagaEvent) {
				this.onEvent(saga, sagaEvent);
				then.onEvent(saga, sagaEvent);
			}
		};
	}

}
