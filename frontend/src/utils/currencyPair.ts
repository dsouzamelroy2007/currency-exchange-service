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

// Deterministic accent color per pair, so cards read as distinct at a glance instead of
// uniform gray. Hue only (fixed saturation/lightness) keeps contrast consistent across pairs.
export function pairAccentColor(pair: CurrencyPair): string {
  const key = pairKey(pair);
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
  }
  return `hsl(${hash % 360}, 65%, 45%)`;
}
