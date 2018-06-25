package com.djs.saga.core.branch;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.djs.saga.core.branch.builder.Waiter;
import com.djs.saga.core.step.Step;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class BranchImpl<AWAITED_VALUE> implements Branch {

	private final String name;

	private final Waiter<AWAITED_VALUE> waiter;

	private final Step<AWAITED_VALUE> step;

	@Override
	public BranchOutput await(String parentName, UUID correlationId) {
		String fqn = String.format("%s -> BRANCH(%s)", parentName, name);
		log.debug("The branch [{}] was started", fqn);

		CompletableFuture<BranchPromise> externalisedFuture = new CompletableFuture<>();
		BranchOutput branchOutput = new BranchOutput(
				name,
				fqn,
				externalisedFuture
		);
		CompletableFuture<AWAITED_VALUE> internalFuture = waiter.await(correlationId);

		// When our internal future completes log the output
		internalFuture.whenComplete((v, t) -> {
			if (externalisedFuture.isDone()) {
				return; // We log this case elsewhere
			}

			if (t != null) {
				log.error("The branch [{}] completed exceptionally with error : {}", fqn, t);
			} else {
				log.debug("The branch [{}] completed successfully with result [{}]", fqn, v);
			}
		});

		// When our internal future completes, pass on the outcome to the externalised future
		internalFuture.whenComplete((v, t) -> {
			if (t != null) {
				externalisedFuture.completeExceptionally(t);
			} else {
				externalisedFuture.complete(new BranchPromise(
						step == null ? null : id -> step.run(fqn, id, v)
				));
			}
		});

		// When the externalised future completes, complete our internal future, we do this by cancelling our internal
		// future, however it is IMPORTANT to note that if the internal future has completed already, this will have no
		// effect
		externalisedFuture.whenComplete((b, t) -> {
			if(externalisedFuture.isCancelled()){
				if (internalFuture.cancel(true)) {
					log.debug("The branch [{}] was cancelled as another branch completed first.", fqn);
				}
			}else{
				if (internalFuture.completeExceptionally(t)) {
					log.debug("The branch [{}] was cancelled as another branch completed unsuccessfully.", fqn);
				}
			}
		});

		return branchOutput;
	}

	@Value
	public static class BranchFuture<AWAITED_VALUE, MAPPED_VALUE> {

		CompletableFuture<AWAITED_VALUE> original;
		CompletableFuture<MAPPED_VALUE> instrumented;

	}

}
