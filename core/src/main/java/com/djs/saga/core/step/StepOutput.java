package com.djs.saga.core.step;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.djs.saga.core.branch.BranchOutput;

import lombok.Value;

@Value
public class StepOutput {

	String stepName;
	String stepFqn;
	UUID sagaId;
	UUID stepId;
	Collection<BranchOutput> branches;
	CompletableFuture<StepPromise> promise;

}
