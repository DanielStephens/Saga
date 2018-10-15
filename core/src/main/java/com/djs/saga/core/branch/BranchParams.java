package com.djs.saga.core.branch;

import java.util.UUID;

import com.djs.saga.core.display.SagaToStringBuilder;

import lombok.Value;

@Value
public class BranchParams {

	SagaToStringBuilder toStringBuilder;
	UUID sagaId;
	UUID stepId;

}
