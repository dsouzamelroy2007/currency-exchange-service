export interface CurrencyPair {
  from: string;
  to: string;
}

export function pairKey(pair: CurrencyPair): string {
  return `${pair.from.toUpperCase()}_${pair.to.toUpperCase()}`;
}

export function normalizePair(from: string, to: string): CurrencyPair {
  return { from: from.trim().toUpperCase(), to: to.trim().toUpperCase() };
}
