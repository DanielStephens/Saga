package com.djs.saga.core.saga;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
public class SagaOutput<SAGA_INPUT> {

	@Getter
	Saga<SAGA_INPUT> saga;
	@Getter
	UUID correlationId;
	@Getter
	SAGA_INPUT input;
	CompletableFuture<Void> promise;

	/**
	 * Throws an (unchecked) exception if completed exceptionally. If a computation involved in the completion of this
	 * {@link SagaOutput#saga} threw an exception, this method throws an (unchecked) {@link CompletionException} with
	 * the underlying exception as its cause.
	 *
	 * @throws CancellationException if the computation was cancelled
	 * @throws CompletionException if this future completed
	 * exceptionally or a completion computation threw an exception
	 */
	public void block(){
		promise.join();
	}

	/**
	 * If not already completed, completes this {@link SagaOutput#saga} with a {@link CancellationException}. If this
	 * method returns {@code true} then subsequent calls to {@link SagaOutput#block()} will throw a
	 * {@link CancellationException}.
	 *
	 * @return {@code true} if this task is now cancelled
	 */
	public boolean cancel(){
		return promise.cancel(true);
	}

}
