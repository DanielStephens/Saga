package com.djs.saga.prefabs.actions;

import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionOperations;

import com.djs.saga.core.step.builder.Action;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageActions {

	private final TransactionOperations txTemplate;

	public <STEP_INPUT> SendMessage<STEP_INPUT> sendMessage(MessageConverter<STEP_INPUT> messageConverter) {
		return new SendMessage<>(txTemplate, messageConverter);
	}

	public <STEP_INPUT> SendMessages<STEP_INPUT> sendMessages(MessagesConverter<STEP_INPUT> messagesConverter) {
		return new SendMessages<>(txTemplate, messagesConverter);
	}

	@AllArgsConstructor
	public static class SendMessage<STEP_INPUT> {

		private final TransactionOperations txTemplate;
		private final MessageConverter<STEP_INPUT> messageConverter;

		public Action<STEP_INPUT> to(MessageChannel messageChannel) {
			return (correlationId, stepInput) -> {
				Message<?> message = messageConverter.convert(correlationId, stepInput);
				txTemplate.execute(s -> messageChannel.send(message));
			};
		}

	}

	@AllArgsConstructor
	public static class SendMessages<STEP_INPUT> {

		private final TransactionOperations txTemplate;
		private final MessagesConverter<STEP_INPUT> messagesConverter;

		public Action<STEP_INPUT> to(MessageChannel messageChannel) {
			return (correlationId, stepInput) -> {
				Collection<Message<?>> messages = messagesConverter.convert(correlationId, stepInput);
				txTemplate.execute(s -> messages.stream()
						.map(messageChannel::send)
						.reduce(true, Boolean::logicalAnd)
				);
			};
		}

	}

}
