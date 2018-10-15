package com.djs.saga.core.branches;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.djs.saga.core.branch.BranchOutput;
import com.djs.saga.core.branch.BranchPromise;

import lombok.Value;

@Value
public class BranchesOutput {

	UUID sagaId;
	Collection<BranchOutput> branches;
	CompletableFuture<BranchPromise> promise;

}
