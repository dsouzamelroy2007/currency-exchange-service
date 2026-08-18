package com.assignment.sm.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Tracks which currency-pair rate topics currently have at least one WebSocket subscriber,
 * enforces a cap on how many distinct pairs can be tracked concurrently, and publishes a
 * {@link NewRateSubscriptionEvent} the first time a pair gains a subscriber so it can be
 * fetched and pushed immediately instead of waiting for the next scheduled tick.
 */
@Slf4j
@Component
public class RateSubscriptionRegistry implements ChannelInterceptor {

  private static final Pattern RATE_TOPIC_PATTERN = Pattern.compile("^/topic/rates/([^/]+)/([^/]+)$");

  private enum AcquireResult { REJECTED, ACQUIRED_EXISTING, ACQUIRED_NEW }

  @Value("${websocket.maxActivePairs:50}")
  private int maxActivePairs;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  private final ConcurrentHashMap<String, AtomicInteger> subscriptionRefCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> sessionSubscriptions = new ConcurrentHashMap<>();

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    StompCommand command = accessor.getCommand();
    if (StompCommand.SUBSCRIBE.equals(command)) {
      handleSubscribe(accessor);
    } else if (StompCommand.UNSUBSCRIBE.equals(command)) {
      handleUnsubscribe(accessor);
    }
    return message;
  }

  private void handleSubscribe(StompHeaderAccessor accessor) {
    Matcher matcher = matchDestination(accessor.getDestination());
    if (matcher == null) {
      return;
    }
    String from = matcher.group(1);
    String to = matcher.group(2);
    String pairKey = pairKey(from, to);

    AcquireResult result = tryAcquire(pairKey);
    if (result == AcquireResult.REJECTED) {
      throw new MessagingException("Active currency pair limit (" + maxActivePairs + ") reached, cannot subscribe to " + pairKey);
    }

    sessionSubscriptions.computeIfAbsent(accessor.getSessionId(), key -> new ConcurrentHashMap<>())
        .put(accessor.getSubscriptionId(), pairKey);
    log.info("Subscribed to rate topic {} (session {}, subscription {})", pairKey, accessor.getSessionId(), accessor.getSubscriptionId());

    if (result == AcquireResult.ACQUIRED_NEW) {
      eventPublisher.publishEvent(new NewRateSubscriptionEvent(this, from, to));
    }
  }

  private void handleUnsubscribe(StompHeaderAccessor accessor) {
    ConcurrentHashMap<String, String> subscriptions = sessionSubscriptions.get(accessor.getSessionId());
    if (subscriptions == null) {
      return;
    }
    String pairKey = subscriptions.remove(accessor.getSubscriptionId());
    if (pairKey != null) {
      release(pairKey);
    }
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    ConcurrentHashMap<String, String> subscriptions = sessionSubscriptions.remove(accessor.getSessionId());
    if (subscriptions != null) {
      subscriptions.values().forEach(this::release);
    }
  }

  private synchronized AcquireResult tryAcquire(String pairKey) {
    AtomicInteger existing = subscriptionRefCounts.get(pairKey);
    if (existing != null) {
      existing.incrementAndGet();
      return AcquireResult.ACQUIRED_EXISTING;
    }
    if (subscriptionRefCounts.size() >= maxActivePairs) {
      return AcquireResult.REJECTED;
    }
    subscriptionRefCounts.put(pairKey, new AtomicInteger(1));
    return AcquireResult.ACQUIRED_NEW;
  }

  private void release(String pairKey) {
    subscriptionRefCounts.computeIfPresent(pairKey, (key, count) -> count.decrementAndGet() <= 0 ? null : count);
    log.info("Unsubscribed from rate topic {}", pairKey);
  }

  public Set<String> getActivePairs() {
    return subscriptionRefCounts.keySet();
  }

  public static String pairKey(String fromCurrency, String toCurrency) {
    return fromCurrency + "_" + toCurrency;
  }

  private Matcher matchDestination(String destination) {
    if (destination == null) {
      return null;
    }
    Matcher matcher = RATE_TOPIC_PATTERN.matcher(destination);
    return matcher.matches() ? matcher : null;
  }
}
