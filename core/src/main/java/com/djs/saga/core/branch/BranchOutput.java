package com.djs.saga.core.branch;

import java.util.concurrent.CompletableFuture;

import lombok.Value;

@Value
public class BranchOutput {

	String branchName;
	String branchFqn;
	CompletableFuture<BranchPromise> promise;

}
