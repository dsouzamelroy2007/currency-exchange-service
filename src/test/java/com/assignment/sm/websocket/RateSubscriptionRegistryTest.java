package com.assignment.sm.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith(MockitoExtension.class)
public class RateSubscriptionRegistryTest {

  @InjectMocks
  RateSubscriptionRegistry registry;

  @Mock
  ApplicationEventPublisher eventPublisher;

  @BeforeEach
  public void setUp() throws Exception {
    setMaxActivePairs(50);
  }

  private void setMaxActivePairs(int max) throws Exception {
    Field field = RateSubscriptionRegistry.class.getDeclaredField("maxActivePairs");
    field.setAccessible(true);
    field.set(registry, max);
  }

  private Message<byte[]> subscribeMessage(String sessionId, String subscriptionId, String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId(sessionId);
    accessor.setSubscriptionId(subscriptionId);
    accessor.setDestination(destination);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> unsubscribeMessage(String sessionId, String subscriptionId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    accessor.setSessionId(sessionId);
    accessor.setSubscriptionId(subscriptionId);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> disconnectMessage(String sessionId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setSessionId(sessionId);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  public void testFirstSubscribePublishesNewRateSubscriptionEvent(){
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);

    verify(eventPublisher, times(1)).publishEvent(any(NewRateSubscriptionEvent.class));
    assertTrue(registry.getActivePairs().contains("BTC_USD"));
  }

  @Test
  public void testSecondSubscribeToSamePairDoesNotPublishAgain(){
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);
    registry.preSend(subscribeMessage("session-2", "sub-0", "/topic/rates/BTC/USD"), null);

    verify(eventPublisher, times(1)).publishEvent(any(NewRateSubscriptionEvent.class));
  }

  @Test
  public void testUnsubscribeRemovesPairWhenLastSubscriberLeaves(){
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);
    registry.preSend(unsubscribeMessage("session-1", "sub-0"), null);

    assertTrue(registry.getActivePairs().isEmpty());
  }

  @Test
  public void testUnsubscribeKeepsPairWhileOtherSubscribersRemain(){
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);
    registry.preSend(subscribeMessage("session-2", "sub-0", "/topic/rates/BTC/USD"), null);
    registry.preSend(unsubscribeMessage("session-1", "sub-0"), null);

    assertTrue(registry.getActivePairs().contains("BTC_USD"));
  }

  @Test
  public void testDisconnectReleasesAllSubscriptionsForSession(){
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);
    registry.preSend(subscribeMessage("session-1", "sub-1", "/topic/rates/ETH/USD"), null);

    registry.handleDisconnect(new SessionDisconnectEvent(this, disconnectMessage("session-1"), "session-1", CloseStatus.NORMAL));

    assertTrue(registry.getActivePairs().isEmpty());
  }

  @Test
  public void testSubscribeBeyondCapIsRejected() throws Exception {
    setMaxActivePairs(1);
    registry.preSend(subscribeMessage("session-1", "sub-0", "/topic/rates/BTC/USD"), null);

    assertThrows(MessagingException.class, () -> {
      registry.preSend(subscribeMessage("session-2", "sub-0", "/topic/rates/ETH/USD"), null);
    });
    assertEquals(1, registry.getActivePairs().size());
  }
}
