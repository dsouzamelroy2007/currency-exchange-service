import { useCallback, useState } from 'react';
import { normalizePair, pairKey, type CurrencyPair } from '../utils/currencyPair';

const STORAGE_KEY = 'trackedPairs';
const DEFAULT_PAIRS: CurrencyPair[] = [{ from: 'BTC', to: 'USD' }];

function loadPairs(): CurrencyPair[] {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return DEFAULT_PAIRS;

  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return DEFAULT_PAIRS;

    const pairs = parsed.filter(
      (item): item is CurrencyPair =>
        typeof item === 'object' &&
        item !== null &&
        typeof (item as CurrencyPair).from === 'string' &&
        typeof (item as CurrencyPair).to === 'string',
    );
    return pairs.length > 0 ? pairs : DEFAULT_PAIRS;
  } catch {
    return DEFAULT_PAIRS;
  }
}

function savePairs(pairs: CurrencyPair[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(pairs));
}

export function useTrackedPairs() {
  const [pairs, setPairs] = useState<CurrencyPair[]>(loadPairs);

  const addPair = useCallback((from: string, to: string) => {
    const pair = normalizePair(from, to);
    setPairs((prev) => {
      if (prev.some((p) => pairKey(p) === pairKey(pair))) return prev;
      const next = [...prev, pair];
      savePairs(next);
      return next;
    });
  }, []);

  const removePair = useCallback((pair: CurrencyPair) => {
    setPairs((prev) => {
      const next = prev.filter((p) => pairKey(p) !== pairKey(pair));
      savePairs(next);
      return next;
    });
  }, []);

  return { pairs, addPair, removePair };
}
