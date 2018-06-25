package com.djs.saga.config;

import org.springframework.context.annotation.Import;

@Import(SagaConfig.class)
public @interface EnableSagas {
}
