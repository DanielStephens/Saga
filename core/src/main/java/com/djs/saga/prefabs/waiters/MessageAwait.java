package com.djs.saga.prefabs.waiters;

import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import com.djs.saga.core.branch.builder.Waiter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageAwait {

	public ExpectedMessageCorrelation expectMessage(Predicate<Message<?>> messagePredicate) {
		return new ExpectedMessageCorrelation(messagePredicate);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessageCorrelation {

		private final Predicate<Message<?>> messagePredicate;

		public ExpectedMessageChannel correlatedBy(UUID correlationId) {
			return new ExpectedMessageChannel(messagePredicate, correlationId);
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessageChannel {

		private final Predicate<Message<?>> messagePredicate;
		private final UUID correlationId;

		public Waiter<Message<?>> on(SubscribableChannel subscribableChannel) {
			return () -> {
				CompletableFuture<Message<?>> future = new CompletableFuture<>();

				MessageHandler messageHandler = msg -> {
					if(!correlationId.equals(msg.getHeaders().get(CORRELATION_ID, UUID.class))){
						log.trace("Ignoring message [{}] on channel [{}] as it does not have the expected correlationId [{}].", msg, subscribableChannel, correlationId);
						return;
					}

					if (messagePredicate.test(msg)) {
						log.debug("Completing waiter with message [{}] on channel [{}]", msg, subscribableChannel);
						future.complete(msg);
					}else{
						log.trace("Ignoring message [{}] on channel [{}] as it does not match the predicate.", msg, subscribableChannel);
					}
				};

				subscribableChannel.subscribe(messageHandler);
				future.whenComplete((m, t) -> subscribableChannel.unsubscribe(messageHandler));
				return future;
			};
		}

	}

	public ExpectedMessageCorrelated expectMessages(Predicate<MessageGroup> messageGroupPredicate) {
		return new ExpectedMessageCorrelated(messageGroupPredicate);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessageCorrelated {

		private final Predicate<MessageGroup> messageGroupPredicate;

		public ExpectedMessagesChannel correlatedBy(UUID correlationId) {
			return new ExpectedMessagesChannel(messageGroupPredicate, correlationId);
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessagesChannel {

		private final Predicate<MessageGroup> messageGroupPredicate;
		private final UUID correlationId;

		public ExpectedMessagesStorage on(SubscribableChannel subscribableChannel) {
			return new ExpectedMessagesStorage(messageGroupPredicate, correlationId, subscribableChannel);
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class ExpectedMessagesStorage {

		private final Predicate<MessageGroup> messageGroupPredicate;
		private final UUID correlationId;
		private final SubscribableChannel subscribableChannel;

		public Waiter<MessageGroup> usingStore(MessageGroupStore messageGroupStore) {
			return () -> {
				CompletableFuture<MessageGroup> future = new CompletableFuture<>();

				MessageHandler messageHandler = msg -> {
					UUID id = msg.getHeaders().get(CORRELATION_ID, UUID.class);
					if (!correlationId.equals(id)) {
						return;
					}

					synchronized (messageGroupStore) {
						messageGroupStore.addMessagesToGroup(correlationId, msg);
						MessageGroup messageGroup = messageGroupStore.getMessageGroup(correlationId);
						if (messageGroupPredicate.test(messageGroup)) {
							future.complete(messageGroup);
						}
					}
				};

				subscribableChannel.subscribe(messageHandler);
				future.whenComplete((m, t) -> subscribableChannel.unsubscribe(messageHandler));
				return future;
			};
		}

	}


}
