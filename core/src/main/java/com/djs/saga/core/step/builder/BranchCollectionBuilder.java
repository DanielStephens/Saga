package com.djs.saga.core.step.builder;

import java.util.Collection;
import java.util.UUID;

import com.djs.saga.core.branch.Branch;

@FunctionalInterface
public interface BranchCollectionBuilder<STEP_INPUT> {

	Collection<Branch> build(UUID correlationId, STEP_INPUT stepInput, Action<STEP_INPUT> stepAction);

}
