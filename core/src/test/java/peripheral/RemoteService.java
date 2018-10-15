package peripheral;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteService implements SubscribableChannel {

	private static ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(4);
	private final String name;
	private final AtomicBoolean isRunning;
	private final Collection<Future<?>> inflightMessages;
	private final int millisecondsDelay;
	private final SubscribableChannel responseChannel;

	public RemoteService(String name, float responseSecondsDelay) {
		this.name = name;
		this.isRunning = new AtomicBoolean(true);
		this.inflightMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.millisecondsDelay = (int) (responseSecondsDelay * 1000);
		this.responseChannel = new PublishSubscribeChannel();
	}

	public Collection<Message<?>> respond(Message<?> request) {
		if (!(request.getPayload() instanceof ServiceRequest)) {
			return Collections.emptyList();
		}

		ServiceRequest r = (ServiceRequest) request.getPayload();

		return Collections.singleton(MessageBuilder.withPayload(new ServiceResponse(
				r,
				r.isValidRequest()
		)).copyHeaders(request.getHeaders()).build());
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		log.debug("A message handler is being subscribed to service [{}]", name);
		return responseChannel.subscribe(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		log.debug("A message handler is being unsubscribed from service [{}]", name);
		return responseChannel.unsubscribe(handler);
	}

	@Override
	public synchronized boolean send(Message<?> message) {
		return send(message, Long.MAX_VALUE);
	}

	@Override
	public synchronized boolean send(Message<?> message, long timeout) {
		if (!isRunning.get()) {
			log.warn("The service [{}] was sent the message [{}] but is currently not running.", name, message);
			return false;
		}

		log.debug("The message [{}] is being sent to service [{}]", message, name);

		CompletableFuture<?> f = new CompletableFuture<>();

		ScheduledFuture<?> schedule = EXECUTOR.schedule(
				() -> {
					respond(message).forEach(responseChannel::send);
					inflightMessages.remove(f);
				},
				millisecondsDelay, TimeUnit.MILLISECONDS
		);

		f.whenComplete((v, t) -> schedule.cancel(true));
		inflightMessages.add(f);
		return true;
	}

	/**
	 * @return {@code true} if the service was crashed, {@code false} if the service was already crashed.
	 */
	public synchronized boolean crash() {
		if (isRunning.get()) {
			isRunning.set(false);
			int size = inflightMessages.size();
			inflightMessages.forEach(f -> f.cancel(true));
			inflightMessages.clear();
			log.warn("The service [{}] has crashed and {} in flight messages were lost.", name, size);
			return true;
		} else {
			log.trace("The service [{}] was already crashed so calling crash again will have no effect", name);
			return false;
		}
	}

	/**
	 * @return {@code true} if the service was restarted, {@code false} if the service was already running.
	 */
	public synchronized boolean restart() {
		if (isRunning.get()) {
			log.trace("The service [{}] was already running so calling restart again will have no effect", name);
			return false;
		} else {
			log.trace("The service [{}] is being restarted.", name);
			isRunning.set(true);
			return true;
		}
	}

	@Value
	public static class ServiceRequest {

		boolean validRequest;

	}

	@Value
	public static class ServiceResponse {

		ServiceRequest request;
		boolean bookingSuccessful;

	}

}
