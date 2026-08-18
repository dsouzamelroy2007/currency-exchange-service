import { currencySymbol } from '../../currencies/knownCurrencies';
import { useLiveRate } from '../../hooks/useLiveRate';
import { pairAccentColor, type CurrencyPair } from '../../utils/currencyPair';

interface PairCardProps {
  pair: CurrencyPair;
  onRemove: (pair: CurrencyPair) => void;
}

export function PairCard({ pair, onRemove }: PairCardProps) {
  const rate = useLiveRate(pair.from, pair.to);
  const accent = pairAccentColor(pair);

  return (
    <div className="pair-card" style={{ '--pair-accent': accent } as React.CSSProperties}>
      <div className="pair-card-header">
        <h3>
          <span className="pair-card-symbol">{currencySymbol(pair.from)}</span>
          {pair.from}/{pair.to}
          <span className="pair-card-symbol">{currencySymbol(pair.to)}</span>
        </h3>
        <button type="button" onClick={() => onRemove(pair)} aria-label={`Remove ${pair.from}/${pair.to}`}>
          ×
        </button>
      </div>
      {rate ? (
        <>
          <p className="pair-card-rate">
            {currencySymbol(pair.to)} {rate.exchangeRate}
          </p>
          <p className="pair-card-updated">Last updated: {rate.date}</p>
        </>
      ) : (
        <p className="pair-card-loading">Waiting for rate…</p>
      )}
    </div>
  );
}
