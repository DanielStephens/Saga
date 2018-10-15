package com.djs.saga.prefabs.compensators;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.djs.saga.core.branch.builder.DeadEnd;
import com.djs.saga.core.branch.builder.Compensator;
import com.djs.saga.core.branch.builder.DeadEndCompensator;
import com.djs.saga.core.step.builder.Action;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Compensators {

	public <INPUT> RetryBuilderValue<INPUT> retry(Action<INPUT> action) {
		return new RetryBuilderValue<>(action);
	}

	@AllArgsConstructor
	public static class RetryBuilderValue<INPUT> {

		private final Action<INPUT> action;

		public RetryBuilderCondition<INPUT> withValue(INPUT input) {
			return withValue(() -> input);
		}

		public RetryBuilderCondition<INPUT> withValue(Supplier<INPUT> inputSupplier) {
			return new RetryBuilderCondition<>(action, inputSupplier);
		}

	}

	@AllArgsConstructor
	public static class RetryBuilderCondition<INPUT> {

		private final Action<INPUT> action;
		private final Supplier<INPUT> inputSupplier;

		public <T> Compensator<T> times(int times) {
			return conditionalOn(() -> {
				AtomicInteger counter = new AtomicInteger(times);
				return () -> counter.getAndDecrement() > 0;
			});
		}

		public <T> DeadEndCompensator<T> forever() {
			return asDeadEnd(conditionalOn(() -> () -> true));
		}

		private <T> DeadEndCompensator<T> asDeadEnd(Compensator<T> compensation) {
			return waiter -> correlationId -> {
				CompletableFuture<T> f1 = compensation.instrument(waiter).await(correlationId);
				CompletableFuture<DeadEnd> f2 = f1.thenApply(f -> DeadEnd.INSTANCE);
				f2.whenComplete((v, t) -> f1.cancel(true));
				return f2;
			};
		}

		public <T> Compensator<T> conditionalOn(Supplier<Supplier<Boolean>> retryConditionSupplier) {
			return waiter -> correlationId -> {
				UUID compensationId = UUID.randomUUID();
				AtomicInteger counter = new AtomicInteger(0);
				CompletableFuture<T> future = new CompletableFuture<>();
				Supplier<Boolean> shouldRetry = retryConditionSupplier.get();

				Consumer<CompletableFuture<T>> instrumenter = new Consumer<CompletableFuture<T>>() {
					@Override
					public void accept(CompletableFuture<T> inputCompletableFuture) {
						future.whenComplete((v, t) -> inputCompletableFuture.cancel(true));

						inputCompletableFuture.whenComplete((i, t) -> {
							if (t != null) {
								future.completeExceptionally(t);
							} else if (shouldRetry.get()) {
								log.trace("Performing a compensation action [{}]. Number of times previously performed = {}", compensationId, counter.getAndIncrement());
								this.accept(waiter.await(correlationId));
								action.perform(correlationId, inputSupplier.get());
							} else {
								log.trace("No longer performing the compensation action [{}]. Number of times previously performed = {}", compensationId, counter.get());
								future.complete(i);
							}
						});
					}
				};
				instrumenter.accept(waiter.await(correlationId));
				return future;
			};
		}

	}

}
