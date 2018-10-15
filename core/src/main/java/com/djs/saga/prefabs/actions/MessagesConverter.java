package com.djs.saga.prefabs.actions;

import java.util.Collection;
import java.util.UUID;

import org.springframework.messaging.Message;

public interface MessagesConverter<STEP_INPUT> {

	Collection<Message<?>> convert(UUID correlationId, STEP_INPUT sagaInput);

}
