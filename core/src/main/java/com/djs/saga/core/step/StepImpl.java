package com.djs.saga.core.step;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.djs.saga.core.branches.Branches;
import com.djs.saga.core.branches.BranchesOutput;
import com.djs.saga.core.branches.BranchesParams;
import com.djs.saga.core.display.SagaToStringBuilder;
import com.djs.saga.core.step.builder.Action;
import com.djs.saga.core.step.builder.BranchesBuilder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class StepImpl<STEP_INPUT> implements Step<STEP_INPUT> {

	private static final String LINE_SEPARATOR = System.lineSeparator();
	private final ExecutorService executorService;
	private final String name;
	private final Action<STEP_INPUT> action;
	private final BranchesBuilder<STEP_INPUT> branchesBuilder;

	@Override
	public StepOutput run(StepParams stepParams, STEP_INPUT stepInput) {
		UUID sagaId = stepParams.getSagaId();
		UUID correlationId = UUID.randomUUID();
		SagaToStringBuilder toStringBuilder = stepParams.getToStringBuilder().appendStep(name, stepInput);
		String fqn = toStringBuilder.build();

		log.debug("[SAGA-{}] Step will be run with input [{}].{}{}", sagaId, stepInput, LINE_SEPARATOR, fqn);

		Branches branches = branchesBuilder.build(correlationId, stepInput, action);
		BranchesOutput branchesOutput = branches.await(new BranchesParams(toStringBuilder, sagaId, correlationId));
		executorService.submit(() -> action.perform(correlationId, stepInput));

		CompletableFuture<StepPromise> promise = branchesOutput.getPromise().thenApply(b -> new StepPromise(b.getNextStep()));
		promise.whenComplete((p, t) -> {
			if (branchesOutput.getPromise().isDone()) {
				if (t != null) {
					log.error("[SAGA-{}] Step completed exceptionally.{}{}{}{}", sagaId, LINE_SEPARATOR, fqn, LINE_SEPARATOR, t);
				} else {
					log.debug("[SAGA-{}] Step completed successfully and {} have a next step.{}{}", sagaId, p.getBakedStep() == null ? "does not" : "does", LINE_SEPARATOR, fqn);
				}
				return;
			}
			branchesOutput.getPromise().cancel(true);
		});

		return new StepOutput(
				name,
				fqn,
				sagaId,
				correlationId,
				branchesOutput.getBranches(),
				promise
		);
	}
}
