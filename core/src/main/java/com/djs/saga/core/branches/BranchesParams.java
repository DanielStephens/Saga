package com.djs.saga.core.branches;

import java.util.UUID;

import com.djs.saga.core.display.SagaToStringBuilder;

import lombok.Value;

@Value
public class BranchesParams {

	SagaToStringBuilder toStringBuilder;
	UUID sagaId;
	UUID stepId;

}
