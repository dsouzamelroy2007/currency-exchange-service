import { useState } from 'react';
import { CurrencyCombobox } from './CurrencyCombobox';

interface CurrencyPairPickerProps {
  initialFrom?: string;
  initialTo?: string;
  submitLabel: string;
  onSubmit: (from: string, to: string) => void;
}

export function CurrencyPairPicker({
  initialFrom = '',
  initialTo = '',
  submitLabel,
  onSubmit,
}: CurrencyPairPickerProps) {
  const [from, setFrom] = useState(initialFrom);
  const [to, setTo] = useState(initialTo);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!from || !to) {
      setError('Both currencies are required.');
      return;
    }
    if (from.toUpperCase() === to.toUpperCase()) {
      setError('From and To currencies must be different.');
      return;
    }
    setError(null);
    onSubmit(from, to);
  };

  return (
    <form className="currency-pair-picker" onSubmit={handleSubmit}>
      <CurrencyCombobox label="From" value={from} onChange={setFrom} />
      <CurrencyCombobox label="To" value={to} onChange={setTo} />
      <button type="submit">{submitLabel}</button>
      {error && <p className="currency-pair-picker-error">{error}</p>}
    </form>
  );
}
