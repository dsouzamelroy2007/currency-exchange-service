export interface CurrencyExchangeRate {
  fromCurrency: string;
  toCurrency: string;
  exchangeRate: number;
  date: string;
}

export interface ExchangeRateResponse {
  exchangeRates: CurrencyExchangeRate[];
}

export interface ErrorDetails {
  localDateTime: string;
  message: string;
  details: string;
}

export type ApiError =
  | { kind: 'http'; status: number; details: ErrorDetails | null; rawBody: string }
  | { kind: 'gateway-timeout'; rawBody: string }
  | { kind: 'network'; cause: unknown };
