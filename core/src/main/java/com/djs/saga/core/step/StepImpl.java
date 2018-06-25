package com.djs.saga.core.step;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.djs.saga.core.branches.Branches;
import com.djs.saga.core.branches.BranchesOutput;
import com.djs.saga.core.step.builder.Action;
import com.djs.saga.core.step.builder.BranchesBuilder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class StepImpl<STEP_INPUT> implements Step<STEP_INPUT> {

	private final ExecutorService executorService;
	private final String name;
	private final Action<STEP_INPUT> action;
	private final BranchesBuilder<STEP_INPUT> branchesBuilder;

	@Override
	public StepOutput run(String parentName, UUID correlationId, STEP_INPUT stepInput) {
		String fqn = String.format("%s -> STEP(%s)", parentName, name);
		log.debug("The step [{}] is being run with input [{}].", fqn, stepInput);

		Branches branches = branchesBuilder.build(correlationId, stepInput, action);
		BranchesOutput branchesOutput = branches.await(fqn, correlationId);
		executorService.submit(() -> action.perform(correlationId, stepInput));

		CompletableFuture<StepPromise> promise = branchesOutput.getPromise().thenApply(b -> new StepPromise(b.getNextStep()));
		promise.whenComplete((p, t) -> {
			if (branchesOutput.getPromise().isDone()) {
				if (t != null) {
					log.error("The step [{}] was completed exceptionally with the error : {}", fqn, t);
				} else {
					log.debug("The step [{}] was completed successfully and {} have a next step", fqn, p.getBakedStep() == null ? "does not" : "does");
				}
				return;
			}

			log.debug("The step [{}] is being completed externally, the associated branches will be canceled.", fqn);
			branchesOutput.getPromise().cancel(true);
		});

		return new StepOutput(
				name,
				fqn,
				branchesOutput.getBranches(),
				promise
		);
	}
}
