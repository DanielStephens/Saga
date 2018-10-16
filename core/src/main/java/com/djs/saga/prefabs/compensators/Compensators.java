package com.djs.saga.prefabs.compensators;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.djs.saga.core.branch.builder.DeadEnd;
import com.djs.saga.core.branch.builder.Compensator;
import com.djs.saga.core.branch.builder.DeadEndCompensator;
import com.djs.saga.core.step.builder.Action;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Compensators {

	public <AWAITED_VALUE> DeadEndCompensator<AWAITED_VALUE> fail(Function<AWAITED_VALUE, Exception> exceptionBuilder) {
		return waiter -> () -> {
			CompletableFuture<AWAITED_VALUE> f1 = waiter.await();
			CompletableFuture<DeadEnd> f2 = new CompletableFuture<>();

			f1.whenComplete((a, t) -> {
				if(t != null){
					f2.completeExceptionally(t);
				}else{
					f2.completeExceptionally(exceptionBuilder.apply(a));
				}
			});

			f2.whenComplete((d, t) -> f1.cancel(true));
			return f2;
		};
	}

	public <INPUT> PerformBuilderValue<INPUT> perform(UUID correlationId, Action<INPUT> action) {
		return new PerformBuilderValue<>(correlationId, action);
	}

	public PerformBuilderCondition perform(Runnable runnable) {
		return new PerformBuilderCondition(runnable);
	}

	@AllArgsConstructor
	public static class PerformBuilderValue<INPUT> {

		private final UUID correlationId;
		private final Action<INPUT> action;

		public PerformBuilderCondition withValue(INPUT input) {
			return withValue(() -> input);
		}

		public PerformBuilderCondition withValue(Supplier<INPUT> inputSupplier) {
			return new PerformBuilderCondition(() -> action.perform(correlationId, inputSupplier.get()));
		}

	}

	@AllArgsConstructor
	public static class PerformBuilderCondition {

		private final Runnable runnable;

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
			return waiter -> () -> {
				CompletableFuture<T> f1 = compensation.instrument(waiter).await();
				CompletableFuture<DeadEnd> f2 = f1.thenApply(f -> DeadEnd.INSTANCE);
				f2.whenComplete((v, t) -> f1.cancel(true));
				return f2;
			};
		}

		public <T> Compensator<T> conditionalOn(Supplier<Supplier<Boolean>> retryConditionSupplier) {
			return waiter -> () -> {
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
								this.accept(waiter.await());
								runnable.run();
							} else {
								log.trace("No longer performing the compensation action [{}]. Number of times previously performed = {}", compensationId, counter.get());
								future.complete(i);
							}
						});
					}
				};
				instrumenter.accept(waiter.await());
				return future;
			};
		}

	}

}
