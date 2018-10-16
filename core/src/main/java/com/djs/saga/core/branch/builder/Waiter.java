package com.djs.saga.core.branch.builder;

import java.util.concurrent.CompletableFuture;

public interface Waiter<AWAITED_VALUE> {

	CompletableFuture<AWAITED_VALUE> await();

}
