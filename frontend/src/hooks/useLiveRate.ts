import { useEffect, useState } from 'react';
import { getLiveRate } from '../api/exchangeRateClient';
import type { CurrencyExchangeRate } from '../api/types';
import { stompConnectionManager } from '../ws/stompConnectionManager';

export function useLiveRate(from: string, to: string) {
  const [rate, setRate] = useState<CurrencyExchangeRate | null>(null);

  useEffect(() => {
    let cancelled = false;
    setRate(null);

    getLiveRate(from, to)
      .then((initial) => {
        if (!cancelled) setRate(initial);
      })
      .catch(() => {
        // WS push (or the next successful poll) will populate the rate instead.
      });

    const unsubscribe = stompConnectionManager.subscribe(from, to, (update) => {
      if (!cancelled) setRate(update);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [from, to]);

  return rate;
}
