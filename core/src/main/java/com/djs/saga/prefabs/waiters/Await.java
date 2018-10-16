package com.djs.saga.prefabs.waiters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.djs.saga.core.branch.builder.Waiter;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class Await {

	private final ScheduledExecutorService scheduledExecutorService;

	public Waiter<Void> nothing() {
		return nothing(null);
	}

	public <AWAITED_VALUE> Waiter<AWAITED_VALUE> nothing(AWAITED_VALUE awaitedValue) {
		return () -> CompletableFuture.completedFuture(awaitedValue);
	}

	public Waiter<Delay> delay(long delay, TimeUnit unit) {
		return () -> {
			Delay d = new Delay(delay, unit);
			CompletableFuture<Delay> future = new CompletableFuture<>();
			ScheduledFuture<Boolean> schedule = scheduledExecutorService.schedule(() -> future.complete(d), delay, unit);
			future.whenComplete((o, t) -> schedule.cancel(true));
			return future;
		};
	}

	@Value
	public static class Delay {

		long delay;
		TimeUnit unit;

	}

}
