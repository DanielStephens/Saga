package com.djs.saga.core.branch;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.Value;

@Value
public class BranchOutput {

	String branchName;
	String branchFqn;
	UUID sagaId;
	CompletableFuture<BranchPromise> promise;

}
