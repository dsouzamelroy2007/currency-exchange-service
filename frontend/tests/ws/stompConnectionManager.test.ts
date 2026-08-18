import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

interface MockSubscription {
  destination: string;
  unsubscribe: ReturnType<typeof vi.fn>;
}

let clientOptions: {
  onConnect?: () => void;
  onWebSocketClose?: () => void;
  onStompError?: () => void;
} = {};
let subscribeMock: ReturnType<typeof vi.fn>;
let clientCtor: ReturnType<typeof vi.fn>;

vi.mock('@stomp/stompjs', () => {
  return {
    Client: vi.fn().mockImplementation((options) => {
      clientOptions = options;
      const instance = {
        connected: false,
        activate: vi.fn(() => {
          instance.connected = true;
          options.onConnect?.();
        }),
        subscribe: subscribeMock,
      };
      return instance;
    }),
  };
});

vi.mock('sockjs-client', () => ({ default: vi.fn() }));

async function freshManager() {
  vi.resetModules();
  subscribeMock = vi.fn((destination: string): MockSubscription => ({
    destination,
    unsubscribe: vi.fn(),
  }));
  const stompjs = await import('@stomp/stompjs');
  clientCtor = stompjs.Client as unknown as ReturnType<typeof vi.fn>;
  const module = await import('../../src/ws/stompConnectionManager');
  return module.stompConnectionManager;
}

describe('stompConnectionManager', () => {
  beforeEach(() => {
    clientOptions = {};
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('lazy-connects once even across multiple subscribe calls', async () => {
    const manager = await freshManager();

    manager.subscribe('BTC', 'USD', () => {});
    manager.subscribe('ETH', 'EUR', () => {});

    expect(clientCtor).toHaveBeenCalledTimes(1);
  });

  it('shares one STOMP subscription across duplicate pair subscribers', async () => {
    const manager = await freshManager();

    const listenerA = vi.fn();
    const listenerB = vi.fn();
    manager.subscribe('BTC', 'USD', listenerA);
    manager.subscribe('BTC', 'USD', listenerB);

    expect(subscribeMock).toHaveBeenCalledTimes(1);
    expect(subscribeMock).toHaveBeenCalledWith('/topic/rates/BTC/USD', expect.any(Function));

    const message = { body: JSON.stringify({ fromCurrency: 'BTC', toCurrency: 'USD', exchangeRate: 1, date: 'x' }) };
    const onMessage = subscribeMock.mock.calls[0][1];
    onMessage(message);

    expect(listenerA).toHaveBeenCalledTimes(1);
    expect(listenerB).toHaveBeenCalledTimes(1);
  });

  it('only sends STOMP unsubscribe when the last local listener drops', async () => {
    const manager = await freshManager();

    const unsubA = manager.subscribe('BTC', 'USD', vi.fn());
    manager.subscribe('BTC', 'USD', vi.fn());

    const stompSub: MockSubscription = subscribeMock.mock.results[0].value;

    unsubA();
    expect(stompSub.unsubscribe).not.toHaveBeenCalled();
  });

  it('sends STOMP unsubscribe once every local listener has dropped', async () => {
    const manager = await freshManager();

    const unsubA = manager.subscribe('BTC', 'USD', vi.fn());
    const unsubB = manager.subscribe('BTC', 'USD', vi.fn());

    const stompSub: MockSubscription = subscribeMock.mock.results[0].value;

    unsubA();
    unsubB();
    expect(stompSub.unsubscribe).toHaveBeenCalledTimes(1);
  });

  it('does not throw on onStompError and resubscribes wanted pairs on reconnect', async () => {
    const manager = await freshManager();

    manager.subscribe('BTC', 'USD', vi.fn());
    expect(subscribeMock).toHaveBeenCalledTimes(1);

    expect(() => clientOptions.onStompError?.()).not.toThrow();

    // Simulate stompjs reconnecting on its own delay.
    clientOptions.onConnect?.();
    expect(subscribeMock).toHaveBeenCalledTimes(2);
  });
});
