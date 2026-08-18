import { useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useHistoricalRate } from '../../hooks/useHistoricalRate';
import { CurrencyPairPicker } from '../pair-picker/CurrencyPairPicker';
import { DateRangePicker } from './DateRangePicker';

function describeError(error: ReturnType<typeof useHistoricalRate>['error']): string {
  if (!error) return '';
  if (error.kind === 'gateway-timeout') {
    return 'The exchange server took too long to respond. Please try again.';
  }
  if (error.kind === 'network') {
    return 'Network error while fetching historical rates.';
  }
  return error.details?.message ?? `Request failed with status ${error.status}.`;
}

export function HistoricalRateChart() {
  const [pair, setPair] = useState<{ from: string; to: string } | null>(null);
  const [range, setRange] = useState<{ startDate: string; endDate: string } | null>(null);
  const { rates, loading, error, fetchRates } = useHistoricalRate();

  const maybeFetch = (nextPair: { from: string; to: string } | null, nextRange: typeof range) => {
    if (nextPair && nextRange) {
      fetchRates(nextPair.from, nextPair.to, nextRange.startDate, nextRange.endDate);
    }
  };

  return (
    <section className="historical-rate-chart">
      <h2>Historical rates</h2>
      <CurrencyPairPicker
        submitLabel="Set pair"
        onSubmit={(from, to) => {
          const nextPair = { from: from.toUpperCase(), to: to.toUpperCase() };
          setPair(nextPair);
          maybeFetch(nextPair, range);
        }}
      />
      <DateRangePicker
        onApply={(startDate, endDate) => {
          const nextRange = { startDate, endDate };
          setRange(nextRange);
          maybeFetch(pair, nextRange);
        }}
      />

      {loading && <p>Loading historical rates…</p>}
      {error && <p className="historical-rate-chart-error">{describeError(error)}</p>}
      {!loading && !error && rates.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={rates}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis domain={['auto', 'auto']} />
            <Tooltip />
            <Line type="monotone" dataKey="exchangeRate" stroke="#1976d2" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      )}
      {!loading && !error && pair && range && rates.length === 0 && <p>No data for this range.</p>}
    </section>
  );
}
