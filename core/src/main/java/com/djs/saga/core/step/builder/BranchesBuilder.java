package com.djs.saga.core.step.builder;

import java.util.UUID;

import com.djs.saga.core.branches.Branches;

@FunctionalInterface
public interface BranchesBuilder<STEP_INPUT> {

	Branches build(UUID correlationId, STEP_INPUT stepInput, Action<STEP_INPUT> stepAction);

}
