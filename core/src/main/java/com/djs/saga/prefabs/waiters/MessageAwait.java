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

	public ExpectedMessagesChannel expectCorrelatedMessages(Predicate<MessageGroup> messageGroupPredicate) {
		return new ExpectedMessagesChannel(messageGroupPredicate);
	}

	public ExpectedMessageChannel expectMessage(Predicate<Message<?>> messagePredicate) {
		return new ExpectedMessageChannel((id, msg) -> messagePredicate.test(msg));
	}

	public ExpectedMessageChannel expectMessage(BiPredicate<UUID, Message<?>> messagePredicate) {
		return new ExpectedMessageChannel(messagePredicate);
	}

	public ExpectedMessageChannel expectCorrelatedMessage(Predicate<Message<?>> messagePredicate) {
		return new ExpectedMessageChannel((id, msg) -> id.equals(msg.getHeaders().get(CORRELATION_ID, UUID.class)) && messagePredicate.test(msg));
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessageChannel {

		private final BiPredicate<UUID, Message<?>> messagePredicate;

		public Waiter<Message<?>> on(SubscribableChannel subscribableChannel) {
			return correlationId -> {
				CompletableFuture<Message<?>> future = new CompletableFuture<>();

				MessageHandler messageHandler = msg -> {
					if (messagePredicate.test(correlationId, msg)) {
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

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessageCorrelated {

		private final Predicate<MessageGroup> messageGroupPredicate;

		public ExpectedMessagesChannel correlatedBy(CorrelationStrategy correlationStrategy) {
			return new ExpectedMessagesChannel(messageGroupPredicate);
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ExpectedMessagesChannel {

		private final Predicate<MessageGroup> messageGroupPredicate;

		public ExpectedMessagesStorage on(SubscribableChannel subscribableChannel) {
			return new ExpectedMessagesStorage(messageGroupPredicate, subscribableChannel);
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class ExpectedMessagesStorage {

		private final Predicate<MessageGroup> messageGroupPredicate;
		private final SubscribableChannel subscribableChannel;

		public Waiter<MessageGroup> usingStore(MessageGroupStore messageGroupStore) {
			return correlationId -> {
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
