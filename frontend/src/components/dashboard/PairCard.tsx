import { useLiveRate } from '../../hooks/useLiveRate';
import type { CurrencyPair } from '../../utils/currencyPair';

interface PairCardProps {
  pair: CurrencyPair;
  onRemove: (pair: CurrencyPair) => void;
}

export function PairCard({ pair, onRemove }: PairCardProps) {
  const rate = useLiveRate(pair.from, pair.to);

  return (
    <div className="pair-card">
      <div className="pair-card-header">
        <h3>
          {pair.from}/{pair.to}
        </h3>
        <button type="button" onClick={() => onRemove(pair)} aria-label={`Remove ${pair.from}/${pair.to}`}>
          ×
        </button>
      </div>
      {rate ? (
        <>
          <p className="pair-card-rate">{rate.exchangeRate}</p>
          <p className="pair-card-updated">Last updated: {rate.date}</p>
        </>
      ) : (
        <p className="pair-card-loading">Waiting for rate…</p>
      )}
    </div>
  );
}
