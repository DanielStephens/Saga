package com.djs.saga.core.saga.lifecycle;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

public class SagaEvents {

	public static OnBuildEvent ON_BUILD_EVENT = new OnBuildEvent();
	public static StartRecoveryEvent START_RECOVERY_EVENT = new StartRecoveryEvent();

	private SagaEvents() {
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class OnBuildEvent extends SagaEvent {
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class StartRecoveryEvent extends SagaEvent {
	}

	public static abstract class SagaEvent {

		private SagaEvent() {
		}

		public Optional<OnBuildEvent> asOnBuildEvent() {
			return as(OnBuildEvent.class);
		}

		public Optional<StartRecoveryEvent> asStartRecoveryEvent() {
			return as(StartRecoveryEvent.class);
		}

		private <T extends SagaEvent> Optional<T> as(Class<T> clazz) {
			if (clazz.isAssignableFrom(this.getClass())) {
				return Optional.of(clazz.cast(this));
			} else {
				return Optional.empty();
			}
		}

	}

}
