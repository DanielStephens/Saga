package com.djs.saga.core.branch;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.djs.saga.core.branch.builder.Waiter;
import com.djs.saga.core.display.SagaToStringBuilder;
import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.StepParams;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class BranchImpl<AWAITED_VALUE> implements Branch {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final String name;

	private final Waiter<AWAITED_VALUE> waiter;

	private final Step<AWAITED_VALUE> step;

	@Override
	public BranchOutput await(BranchParams branchParams) {
		UUID sagaId = branchParams.getSagaId();

		SagaToStringBuilder toStringBuilder = branchParams.getToStringBuilder()
				.appendBranch(name);

		String fqn = toStringBuilder.build();
		log.debug("[SAGA-{}] Branch started.{}{}", sagaId, LINE_SEPARATOR, fqn);

		CompletableFuture<BranchPromise> externalisedFuture = new CompletableFuture<>();
		BranchOutput branchOutput = new BranchOutput(
				name,
				fqn,
				sagaId,
				externalisedFuture
		);
		CompletableFuture<AWAITED_VALUE> internalFuture = waiter.await(branchParams.getStepId());

		internalFuture.whenComplete((v, t) -> {
			if (externalisedFuture.isDone()) {
				return;
			}

			if (t != null) {
				log.error("[SAGA-{}] Branch completed exceptionally.{}{}{}{}", sagaId, LINE_SEPARATOR, fqn, LINE_SEPARATOR, t);
			} else {
				log.debug("[SAGA-{}] Branch completed successfully with result [{}].{}{}", sagaId, v, LINE_SEPARATOR, fqn);
			}
		});
		// When our internal future completes, pass on the outcome to the externalised future
		internalFuture.whenComplete((v, t) -> {
			if (t != null) {
				externalisedFuture.completeExceptionally(t);
			} else {
				externalisedFuture.complete(new BranchPromise(
						step == null ? null : () -> step.run(new StepParams(toStringBuilder, sagaId), v)
				));
			}
		});

		// When the externalised future completes, complete our internal future, we do this by cancelling our internal
		// future, however it is IMPORTANT to note that if the internal future has completed already, this will have no
		// effect
		externalisedFuture.whenComplete((b, t) -> {
			if (!internalFuture.isDone()) {
				log.debug("[SAGA-{}] Branch was cancelled as another branch completed first.{}{}", sagaId, LINE_SEPARATOR, fqn);
				internalFuture.cancel(true);
			}
		});

		return branchOutput;
	}

}
