import type { CurrencyExchangeRate } from '../api/types';

export type ConnectionStatus = 'connecting' | 'open' | 'reconnecting';

export type RateListener = (rate: CurrencyExchangeRate) => void;
export type StatusListener = (status: ConnectionStatus) => void;
