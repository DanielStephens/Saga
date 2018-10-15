import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StopWatch;

import com.djs.saga.config.SagaConfig;
import com.djs.saga.core.branch.Branch;
import com.djs.saga.core.branch.builder.BranchBuilder;
import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.builder.SagaBuilder;
import com.djs.saga.core.step.Step;
import com.djs.saga.core.step.builder.Action;
import com.djs.saga.core.step.builder.StepBuilder;
import com.djs.saga.prefabs.actions.MessageActions;
import com.djs.saga.prefabs.compensators.Compensators;
import com.djs.saga.prefabs.waiters.Await;
import com.djs.saga.prefabs.waiters.MessageAwait;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import peripheral.RemoteService;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SagaConfig.class, IntegrationTest.Config.class})
public class IntegrationTest {

	private final RemoteService flightService = new RemoteService("flight service", 1);
	private final RemoteService hotelService = new RemoteService("hotel service", 1);

	@Inject
	Await await;

	@Inject
	MessageAwait messageAwait;

	@Inject
	MessageActions messageActions;

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
		// 1. Build a saga, the first step of our 'holiday booking'
		// saga is built with the bookFlightStep()
		Saga<Boolean> bookingSaga = sagaBuilder.start("holiday booking", 1)
				.withStep(bookFlightStep())
				.build();

		Stopwatch sw = Stopwatch.createStarted();

		// 2. When we run the saga we can pass an input value, in
		// this case we pass a boolean 'false' which indeicates that
		// we send an invalid request.
		bookingSaga.run(false).block();

		sw.stop();

		// We send a message immediately, as our initial message is
		// 'invalid', no connected responses should end our first
		// step. We will therefore wait 5 seconds until our timeout
		// retry takes affect. After 5 seconds we will send a valid
		// request to the flight service.
		// The flight service responses have a delay on 1 second so
		// after 6 seconds we will receive a successful response from
		// the flight service and will move onto the next step.
		// We will send a valid request to the hotel service. As it
		// also has a delay of 1 second, we will receive a successful
		// response on the 7th second.
		// Hence, we assert that the saga ran for approximately 7
		// seconds.
		assertThat(sw.elapsed(TimeUnit.MILLISECONDS), allOf(
				greaterThan(6750L),
				lessThan(7250L)
		));
	}

	private Step<Boolean> bookFlightStep() {
		// 3. the first step in our 'holiday booking' saga is named
		// 'book flight' and will send a message to the
		// 'flightService' and await a successful response.
		return stepBuilder.start("book flight")
				// 4. We define an action which will build a message
				// from the input we receive and send it to the
				// 'flightService'
				.withAction(messageActions.sendMessage(this::messageBuilder).to(flightService))
				// 5. We define some possible outcomes as 'branches'
				.withBranches(this::flightBookingOutcomes)
				.build();
	}

	private Collection<Branch> flightBookingOutcomes(UUID uuid, Boolean valid, Action<Boolean> action) {
		// 6. We create 3 branches for the 'book flight' step, this
		// indicates that there are three possible outcomes of the
		// initial action (sending a message to the flight service)
		return Lists.newArrayList(
				// 7. The first branch specifies our 'happy path'. If
				// we receive a successful response, we should
				// continue on to the next step (attempting to book
				// our hotel).
				branchBuilder.start("valid response received")
						.await(messageAwait.expectCorrelatedMessage(this::isSuccessfulResponse).on(flightService))
						.map(m -> true)
						.progress(bookHotelStep())
						.build(),

				// 8. The second branch occurs if we receive a
				// response telling us that the booking was
				// unsuccessful, in this case we choose to retry the
				// original input, with the original action.
				branchBuilder.start("invalid response received")
						.await(messageAwait.expectCorrelatedMessage(this::isUnsuccessfulResponse).on(flightService))
						.use(m -> log.debug("An invalid message was received, retrying the same message"))
						// retry with original value
						.compensate(compensators.retry(action).withValue(valid).forever())
						.build(),

				// 9. The third branch waits for an amount of time
				// before executing so a similar method could be used
				// to perform a true timeout. In this case rather
				// than giving up after the time limit, we retry the
				// original action, but this time using a different
				// input (the hard coded value of true).
				branchBuilder.start("no response")
						.await(await.delay(5, TimeUnit.SECONDS))
						.use(d -> log.debug("A timeout occurred after [{} {}]", d.getDelay(), d.getUnit()))
						// retry with 'true' value
						.compensate(compensators.retry(action).withValue(true).forever())
						.build()
		);
	}

	private Step<Boolean> bookHotelStep() {
		// 10. The following step is similar but simpler. The action
		// we take is still to send a message, this time to the
		// 'hotelService'. However we only have one branch set up.
		return stepBuilder.start("book hotel")
				.withAction(messageActions.sendMessage(this::messageBuilder).to(hotelService))
				.withBranches(this::hotelBookingOutcomes)
				.build();
	}

	private Collection<Branch> hotelBookingOutcomes(UUID uuid, Boolean valid, Action<Boolean> action) {
		return Lists.newArrayList(
				// 11. Our single branch is slightly risky, as if we
				// do not receive the expected message then we may
				// hang forever awaiting this branch. But this keeps
				// the test slightly simpler.
				branchBuilder.start("valid response received")
						.await(messageAwait.expectCorrelatedMessage(this::isSuccessfulResponse).on(hotelService))
						.map(m -> true)
						.build()
		);
	}

	private boolean isSuccessfulResponse(Message<?> message) {
		// Correct type
		return (message.getPayload() instanceof RemoteService.ServiceResponse)
				// and isBookingSuccessful is true
				&& ((RemoteService.ServiceResponse) message.getPayload()).isBookingSuccessful();
	}

	private boolean isUnsuccessfulResponse(Message<?> message) {
		// Wrong type
		return !(message.getPayload() instanceof RemoteService.ServiceResponse)
				// or isBookingSuccessful is false
				|| !((RemoteService.ServiceResponse) message.getPayload()).isBookingSuccessful();
	}

	private Message<?> messageBuilder(UUID uuid, boolean valid) {
		return MessageBuilder.withPayload(new RemoteService.ServiceRequest(valid))
				.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, uuid)
				.build();
	}

	@Configuration
	public static class Config {

		@Bean
		TransactionOperations txTemplate() {
			TransactionOperations mock = mock(TransactionOperations.class);
			doAnswer(invocationOnMock -> {
				invocationOnMock.getArgumentAt(0, TransactionCallback.class).doInTransaction(null);
				return null;
			}).when(mock).execute(any());
			return mock;
		}

	}

}
