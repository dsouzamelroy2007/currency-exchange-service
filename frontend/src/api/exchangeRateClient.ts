import { apiGet } from './httpClient';
import type { CurrencyExchangeRate, ExchangeRateResponse } from './types';

export async function getLiveRate(from: string, to: string): Promise<CurrencyExchangeRate> {
  const response = await apiGet<ExchangeRateResponse>('/exchange/liveRate', {
    from: from.toUpperCase(),
    to: to.toUpperCase(),
  });
  return response.exchangeRates[0];
}

export async function getHistoricalRate(
  from: string,
  to: string,
  startDate: string,
  endDate: string,
): Promise<CurrencyExchangeRate[]> {
  const response = await apiGet<ExchangeRateResponse>('/exchange/historicalRate', {
    from: from.toUpperCase(),
    to: to.toUpperCase(),
    startDate,
    endDate,
  });
  return response.exchangeRates;
}
