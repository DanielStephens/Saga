package com.djs.saga.core.step;

import java.util.UUID;

public interface BakedStep {

	StepOutput run(UUID correlationId);

}
