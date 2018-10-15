package com.djs.saga.prefabs.actions;

import java.util.UUID;

import org.springframework.messaging.Message;

public interface MessageConverter<STEP_INPUT> {

	Message<?> convert(UUID correlationId, STEP_INPUT sagaInput);

}
