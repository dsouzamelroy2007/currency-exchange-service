import { describe, expect, it } from 'vitest';
import { filterCurrencies, type KnownCurrency } from '../../src/currencies/knownCurrencies';

const LIST: KnownCurrency[] = [
  { code: 'USD', name: 'US Dollar' },
  { code: 'EUR', name: 'Euro' },
  { code: 'BTC', name: 'Bitcoin' },
];

describe('filterCurrencies', () => {
  it('is case-insensitive when matching by code', () => {
    expect(filterCurrencies('usd', LIST)).toEqual([LIST[0]]);
    expect(filterCurrencies('USD', LIST)).toEqual([LIST[0]]);
  });

  it('matches by name as well as code', () => {
    expect(filterCurrencies('dollar', LIST)).toEqual([LIST[0]]);
    expect(filterCurrencies('bitcoin', LIST)).toEqual([LIST[2]]);
  });

  it('returns the full list for an empty query', () => {
    expect(filterCurrencies('', LIST)).toEqual(LIST);
    expect(filterCurrencies('   ', LIST)).toEqual(LIST);
  });

  it('returns an empty array when nothing matches', () => {
    expect(filterCurrencies('zzz', LIST)).toEqual([]);
  });
});
