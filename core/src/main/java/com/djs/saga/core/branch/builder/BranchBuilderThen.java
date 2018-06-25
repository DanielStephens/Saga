package com.djs.saga.core.branch.builder;

import java.util.function.Consumer;
import java.util.function.Function;

import com.djs.saga.core.step.Step;

public class BranchBuilderThen<AWAITED_VALUE> extends BranchBuilderBuild<AWAITED_VALUE> {

	private final String name;
	private final Waiter<AWAITED_VALUE> waiter;

	public BranchBuilderThen(String name, Waiter<AWAITED_VALUE> waiter) {
		super(name, waiter, null);
		this.name = name;
		this.waiter = waiter;
	}

	public BranchBuilderThen<AWAITED_VALUE> use(Consumer<AWAITED_VALUE> consumer) {
		return map(m -> {
			consumer.accept(m);
			return m;
		});
	}

	public <R> BranchBuilderThen<R> map(Function<AWAITED_VALUE, R> map) {
		return new BranchBuilderThen<>(
				name,
				correlationId -> waiter.await(correlationId).thenApply(map)
		);
	}

	public BranchBuilderThen<AWAITED_VALUE> compensate(Compensator<AWAITED_VALUE> compensation) {
		return new BranchBuilderThen<>(
				name,
				compensation.instrument(waiter)
		);
	}

	public BranchBuilderBuild<DeadEnd> compensate(DeadEndCompensator<AWAITED_VALUE> compensation) {
		return new BranchBuilderBuild<>(
				name,
				compensation.instrument(waiter),
				null
		);
	}


	public BranchBuilderBuild<AWAITED_VALUE> progress(Step<AWAITED_VALUE> step) {
		return new BranchBuilderBuild<>(
				name,
				waiter,
				step
		);
	}

}
