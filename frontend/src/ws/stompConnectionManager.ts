import { Client, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { CurrencyExchangeRate } from '../api/types';
import type { ConnectionStatus, RateListener, StatusListener } from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/btc';

interface PairSubscription {
  listeners: Set<RateListener>;
  stompSub: StompSubscription | null;
}

function pairKey(from: string, to: string): string {
  return `${from.toUpperCase()}_${to.toUpperCase()}`;
}

class StompConnectionManager {
  private client: Client | null = null;
  private pairs = new Map<string, PairSubscription>();
  private statusListeners = new Set<StatusListener>();
  private status: ConnectionStatus = 'connecting';

  subscribe(from: string, to: string, onMessage: RateListener): () => void {
    this.ensureConnected();

    const key = pairKey(from, to);
    let pair = this.pairs.get(key);
    if (!pair) {
      pair = { listeners: new Set(), stompSub: null };
      this.pairs.set(key, pair);
    }
    pair.listeners.add(onMessage);
    this.ensureSubscribed(key, pair);

    return () => {
      const current = this.pairs.get(key);
      if (!current) return;
      current.listeners.delete(onMessage);
      if (current.listeners.size === 0) {
        current.stompSub?.unsubscribe();
        this.pairs.delete(key);
      }
    };
  }

  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    listener(this.status);
    return () => this.statusListeners.delete(listener);
  }

  private setStatus(status: ConnectionStatus) {
    this.status = status;
    this.statusListeners.forEach((listener) => listener(status));
  }

  private ensureConnected() {
    if (this.client) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        this.setStatus('open');
        this.resubscribeAll();
      },
      onWebSocketClose: () => {
        this.setStatus('reconnecting');
      },
      onStompError: () => {
        this.pairs.forEach((pair) => {
          pair.stompSub = null;
        });
        this.setStatus('reconnecting');
      },
    });

    this.client = client;
    this.setStatus('connecting');
    client.activate();
  }

  private ensureSubscribed(key: string, pair: PairSubscription) {
    if (pair.stompSub || !this.client?.connected) return;

    const [from, to] = key.split('_');
    pair.stompSub = this.client.subscribe(`/topic/rates/${from}/${to}`, (message) => {
      const rate = JSON.parse(message.body) as CurrencyExchangeRate;
      pair.listeners.forEach((listener) => listener(rate));
    });
  }

  private resubscribeAll() {
    this.pairs.forEach((pair, key) => this.ensureSubscribed(key, pair));
  }
}

export const stompConnectionManager = new StompConnectionManager();
