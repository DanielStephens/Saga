package com.djs.saga.core.branches;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.djs.saga.core.branch.Branch;
import com.djs.saga.core.branch.BranchOutput;
import com.djs.saga.core.branch.BranchParams;
import com.djs.saga.core.branch.BranchPromise;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BranchesImpl implements Branches {

	private final Collection<Branch> branches;

	@Override
	public BranchesOutput await(BranchesParams branchesParams) {
		BranchParams branchParams = new BranchParams(branchesParams.getToStringBuilder(), branchesParams.getSagaId(), branchesParams.getStepId());
		List<BranchOutput> branchOutputs = branches.stream()
				.map(b -> b.await(branchParams))
				.collect(Collectors.toList());

		CompletableFuture[] futures = branchOutputs.stream()
				.map(BranchOutput::getPromise)
				.toArray(CompletableFuture[]::new);

		CompletableFuture<BranchPromise> anyOf = CompletableFuture.anyOf(futures).thenApply(o -> (BranchPromise) o);

		// When any of our branches complete, we should make sure to tidy up all the other branches. It is IMPORTANT to
		// note that we run cancel on all the futures as this will have no effect on any futures that have already
		// completed.
		anyOf.whenComplete((b, t) -> Arrays.stream(futures).forEach(f -> f.cancel(true)));

		return new BranchesOutput(
				branchesParams.getSagaId(),
				branchOutputs,
				anyOf
		);
	}
}
