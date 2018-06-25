package com.djs.saga.core.branch;

import com.djs.saga.core.step.BakedStep;

import lombok.Value;

@Value
public class BranchPromise {

	BakedStep nextStep;

}
