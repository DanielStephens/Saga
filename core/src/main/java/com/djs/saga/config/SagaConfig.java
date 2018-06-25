package com.djs.saga.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionOperations;

import com.djs.saga.core.branch.builder.BranchBuilder;
import com.djs.saga.core.register.Sagas;
import com.djs.saga.core.saga.builder.SagaBuilder;
import com.djs.saga.core.step.builder.StepBuilder;
import com.djs.saga.prefabs.compensators.Compensators;
import com.djs.saga.prefabs.waiters.Await;
import com.djs.saga.prefabs.waiters.MessageAwait;

@Configuration
@Import({SagaConfig.SagaRecoveryConfig.class, SagaConfig.MessageSagaConfig.class})
public class SagaConfig {

	private static final String EXECUTOR_EXECUTOR = "executorExecutor";
	private static final String WAIT_EXECUTOR = "waitExecutor";

	@Value("${saga.wait.thread-pool.size:1}")
	int waitThreadPoolSize;

	@Value("${saga.execute.thread-pool.size:4}")
	int executeThreadPoolSize;

	@Bean
	SagaBuilder sagaBuilder(Sagas sagas, Optional<TransactionOperations> transactionOperations) {
		return new SagaBuilder(sagas, transactionOperations);
	}

	@Bean
	StepBuilder stepBuilder(@Named(EXECUTOR_EXECUTOR) ExecutorService executeExecutor) {
		return new StepBuilder(executeExecutor);
	}

	@Bean
	BranchBuilder branchBuilder() {
		return new BranchBuilder();
	}

	@Bean
	Sagas sagas() {
		return new Sagas();
	}

	@Bean(WAIT_EXECUTOR)
	ScheduledExecutorService waitExecutor() {
		return Executors.newScheduledThreadPool(waitThreadPoolSize);
	}

	@Bean(EXECUTOR_EXECUTOR)
	ExecutorService executeExecutor() {
		return Executors.newFixedThreadPool(executeThreadPoolSize);
	}

	@Bean
	Await await(@Named(WAIT_EXECUTOR) ScheduledExecutorService waitingOnlyExecutor) {
		return new Await(waitingOnlyExecutor);
	}

	@Bean
	Compensators compensators() {
		return new Compensators();
	}

	@Configuration
	@ConditionalOnBean(TransactionOperations.class)
	public static class SagaRecoveryConfig {


	}

	@Configuration
	@ConditionalOnClass(MessageChannel.class)
	public static class MessageSagaConfig {

		@Bean
		MessageAwait messageAwait() {
			return new MessageAwait();
		}

	}

}
