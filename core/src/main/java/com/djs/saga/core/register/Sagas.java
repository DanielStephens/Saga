package com.djs.saga.core.register;

import java.util.HashMap;
import java.util.Map;

import com.djs.saga.core.saga.Saga;
import com.djs.saga.core.saga.SagaId;
import com.djs.saga.core.saga.lifecycle.SagaEvents;

public class Sagas {

	private final Map<SagaId, Saga<?>> sagas = new HashMap<>();

	public <SAGA_INPUT> void register(Saga<SAGA_INPUT> saga){
		sagas.put(saga.getSagaId(), saga);
	}

	public void send(SagaEvents.SagaEvent sagaEvent){
		sagas.values().forEach(s -> s.send(sagaEvent));
	}

}
