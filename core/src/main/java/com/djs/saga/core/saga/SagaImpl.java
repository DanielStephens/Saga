package com.djs.saga.core.saga;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.djs.saga.core.saga.lifecycle.SagaEvents;
import com.djs.saga.core.saga.lifecycle.SagaLifecycleHandler;
import com.djs.saga.core.step.BakedStep;
import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.StepOutput;
import com.djs.saga.core.step.StepPromise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class SagaImpl<SAGA_INPUT> implements Saga<SAGA_INPUT> {

	@Getter
	private final SagaId sagaId;

	private final SagaLifecycleHandler<SAGA_INPUT> lifecycleHandler;

	private final Step<SAGA_INPUT> step;

	@Override
	public SagaOutput<SAGA_INPUT> run(SAGA_INPUT sagaInput) {
		UUID correlationId = UUID.randomUUID();
		String fqn = String.format("SAGA(%s:%s)", sagaId.getName(), sagaId.getVersion());
		SagaInput<SAGA_INPUT> i = new SagaInput<>(
				this,
				correlationId,
				sagaInput
		);
		lifecycleHandler.before(i);

		CompletableFuture<Void> promise = new CompletableFuture<>();
		BakedStep bakedStep = (id) -> step.run(fqn, id, sagaInput);
		Consumer<StepOutput> instrumenter = new Consumer<StepOutput>() {
			@Override
			public void accept(StepOutput stepOutput) {
				CompletableFuture<StepPromise> inputPromise = stepOutput.getPromise();

				promise.whenComplete((v, t) -> {
					if (t != null) {
						inputPromise.completeExceptionally(t);
					} else {
						inputPromise.cancel(true);
					}
				});

				inputPromise.whenComplete((p, t) -> {
					if (t != null) {
						log.error("The step [{}] completed exceptionally so the saga [{}] will complete exceptionally.", stepOutput.getStepFqn(), fqn);
						promise.completeExceptionally(t);
					} else if (p.getBakedStep() != null) {
						StepOutput nextStepOutput = p.getBakedStep().run(UUID.randomUUID());
						log.debug("The step [{}] completed successfully, the next step [{}] of the saga [{}] will now run.", stepOutput.getStepFqn(), nextStepOutput.getStepFqn(), fqn);
						this.accept(nextStepOutput);
					} else {
						log.debug("The step [{}] completed successfully and was the last step of the saga [{}] which will now complete successfully.", stepOutput.getStepFqn(), fqn);
						promise.complete(null);
					}
				});
			}
		};
		instrumenter.accept(bakedStep.run(UUID.randomUUID()));

		promise.whenComplete((v, t) -> lifecycleHandler.after(i));
		return new SagaOutput<>(this, correlationId, sagaInput, promise);
	}

	@Override
	public void send(SagaEvents.SagaEvent sagaEvent) {
		lifecycleHandler.onEvent(this, sagaEvent);
	}

}
