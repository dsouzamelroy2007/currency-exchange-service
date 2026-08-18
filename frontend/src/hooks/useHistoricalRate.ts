import { useCallback, useState } from 'react';
import { getHistoricalRate } from '../api/exchangeRateClient';
import type { ApiError, CurrencyExchangeRate } from '../api/types';

interface HistoricalRateState {
  rates: CurrencyExchangeRate[];
  loading: boolean;
  error: ApiError | null;
}

export function useHistoricalRate() {
  const [state, setState] = useState<HistoricalRateState>({ rates: [], loading: false, error: null });

  const fetchRates = useCallback((from: string, to: string, startDate: string, endDate: string) => {
    setState({ rates: [], loading: true, error: null });
    getHistoricalRate(from, to, startDate, endDate)
      .then((rates) => setState({ rates, loading: false, error: null }))
      .catch((error: ApiError) => setState({ rates: [], loading: false, error }));
  }, []);

  return { ...state, fetchRates };
}
