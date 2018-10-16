package com.djs.saga.core.saga;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.djs.saga.core.display.SagaToStringBuilder;
import com.djs.saga.core.saga.lifecycle.SagaEvents;
import com.djs.saga.core.saga.lifecycle.SagaLifecycleHandler;
import com.djs.saga.core.step.BakedStep;
import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.StepOutput;
import com.djs.saga.core.step.StepParams;
import com.djs.saga.core.step.StepPromise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class SagaImpl<SAGA_INPUT> implements Saga<SAGA_INPUT> {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	@Getter
	private final SagaId sagaId;
	private final SagaLifecycleHandler<SAGA_INPUT> lifecycleHandler;
	private final Step<SAGA_INPUT> step;

	@Override
	public SagaOutput<SAGA_INPUT> run(UUID correlationId, SAGA_INPUT sagaInput) {
		SagaToStringBuilder toStringBuilder = SagaToStringBuilder.start(4)
				.appendSaga(sagaId, sagaInput);
		SagaInput<SAGA_INPUT> i = new SagaInput<>(
				this,
				correlationId,
				sagaInput
		);
		log.debug("[SAGA-{}] Saga will be run with input [{}].{}{}", correlationId, sagaInput, LINE_SEPARATOR, toStringBuilder.build());
		lifecycleHandler.before(i);

		CompletableFuture<Void> promise = new CompletableFuture<>();
		BakedStep bakedStep = () -> step.run(new StepParams(toStringBuilder, correlationId), sagaInput);
		Consumer<StepOutput> instrumenter = new Consumer<StepOutput>() {
			@Override
			public void accept(StepOutput stepOutput) {
				CompletableFuture<StepPromise> inputPromise = stepOutput.getPromise();

				promise.whenComplete((v, t) -> inputPromise.cancel(true));

				inputPromise.whenComplete((p, t) -> {
					if (t != null) {
						promise.completeExceptionally(t);
					} else if (p.getBakedStep() != null) {
						log.debug("[SAGA-{}] Saga completed a step successfully and will progress to the next step.{}{}", correlationId, LINE_SEPARATOR, toStringBuilder.build());
						StepOutput nextStepOutput = p.getBakedStep().run();
						this.accept(nextStepOutput);
					} else {
						promise.complete(null);
					}
				});
			}
		};
		instrumenter.accept(bakedStep.run());

		promise.whenComplete((v, t) -> lifecycleHandler.after(i));
		promise.whenComplete((v, t) -> {
			if(t != null){
				log.error("[SAGA-{}] Saga completed exceptionally.{}{}{}{}", correlationId, LINE_SEPARATOR, toStringBuilder.build(), LINE_SEPARATOR, t);
			}else{
				log.debug("[SAGA-{}] Saga completed successfully.{}{}", correlationId, LINE_SEPARATOR, toStringBuilder.build());
			}
		});
		return new SagaOutput<>(this, correlationId, sagaInput, promise);
	}

	@Override
	public void send(SagaEvents.SagaEvent sagaEvent) {
		lifecycleHandler.onEvent(this, sagaEvent);
	}

}
