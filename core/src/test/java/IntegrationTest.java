import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.djs.saga.config.SagaConfig;
import com.djs.saga.core.branch.Branch;
import com.djs.saga.core.branch.builder.BranchBuilder;
import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.builder.SagaBuilder;
import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.builder.Action;
import com.djs.saga.core.step.builder.StepBuilder;
import com.djs.saga.prefabs.compensators.Compensators;
import com.djs.saga.prefabs.waiters.Await;
import com.djs.saga.prefabs.waiters.MessageAwait;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SagaConfig.class)
public class IntegrationTest {

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

	private final List<Consumer<String>> messageHandlers = new ArrayList<>();

	@Inject
	Await await;

	@Inject
	MessageAwait messageAwait;

	@Inject
	Compensators compensators;

	@Inject
	SagaBuilder sagaBuilder;

	@Inject
	StepBuilder stepBuilder;

	@Inject
	BranchBuilder branchBuilder;

	@Test
	public void test() {
		messageHandlers.add(s -> {
			System.out.println("received [" + s + "]");
		});

		Saga<String> saga = sagaBuilder.start("send messages and await fourth response", 1)
				.withStep(twoSecondDelayMessageStep())
				.build();
		saga.run("payload").block();
	}

	private Step<String> twoSecondDelayMessageStep() {
		return stepBuilder.start("send message with 2 second delay")
				.withAction(this::sendMessageAfter2Seconds)
				.withBranches(this::expectMessage)
				.build();
	}

	private Collection<Branch> expectMessage(UUID correlationId, String input, Action<String> action) {
		Iterator<String> iterator = Lists.newArrayList("secondAttempt", "thirdAttempt", "fourthAttempt").iterator();

		return Lists.newArrayList(
				branchBuilder.start("retry every 5 seconds")
						.await(await.delay(5, TimeUnit.SECONDS))
						.use(v -> log.warn("nothing happened after 5 seconds, going to retry"))
						.compensate(compensators.retry(action).withValue(iterator::next).conditionalOn(() -> iterator::hasNext))
						.build(),

				branchBuilder.start("expect fourth message")
						.await((id) -> {
							CompletableFuture<String> f = new CompletableFuture<>();
							messageHandlers.add(s -> {
								if (s.equals("fourthAttempt")) {
									f.complete(s);
								}
							});
							return f;
						})
						.use(v -> log.debug("successfully got message [" + v + "]"))
						.build()
		);
	}

	private void sendMessageAfter2Seconds(UUID correlationId, String payload) {
		sendMessage(payload, 2);
	}

	private void sendMessage(String payload, int secondsDelay) {
		System.out.println("sending [" + payload + "]");
		executorService.schedule(
				() -> {
					messageHandlers.forEach(c -> c.accept(payload));
					System.out.println("sent [" + payload + "] to [" + messageHandlers.size() + "] handlers");
				},
				secondsDelay, TimeUnit.SECONDS
		);
	}

}
