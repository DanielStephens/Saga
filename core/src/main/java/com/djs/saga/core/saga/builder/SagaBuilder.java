package com.djs.saga.core.saga.builder;

import java.util.Optional;

import org.springframework.transaction.support.TransactionOperations;

import com.djs.saga.core.register.Sagas;
import com.djs.saga.core.saga.SagaId;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SagaBuilder {

	private final Sagas sagas;
	private final Optional<TransactionOperations> transactionOperations;

	public SagaBuilderStep start(String name, int version) {
		return new SagaBuilderStep(sagas, transactionOperations, new SagaId(name, version));
	}

}
