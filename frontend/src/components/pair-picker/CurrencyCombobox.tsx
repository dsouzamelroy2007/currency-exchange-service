import { useId, useState } from 'react';
import { filterCurrencies, KNOWN_CURRENCIES } from '../../currencies/knownCurrencies';

interface CurrencyComboboxProps {
  label: string;
  value: string;
  onChange: (code: string) => void;
}

export function CurrencyCombobox({ label, value, onChange }: CurrencyComboboxProps) {
  const [query, setQuery] = useState(value);
  const [open, setOpen] = useState(false);
  const listId = useId();

  const matches = filterCurrencies(query, KNOWN_CURRENCIES);

  const commit = (raw: string) => {
    const code = raw.trim().toUpperCase();
    if (code) onChange(code);
    setOpen(false);
  };

  return (
    <div className="currency-combobox">
      <label>
        {label}
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => commit(query)}
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
        />
      </label>
      {open && matches.length > 0 && (
        <ul id={listId} className="currency-combobox-list" role="listbox">
          {matches.map((currency) => (
            <li key={currency.code} role="option" aria-selected={currency.code === value}>
              <button
                type="button"
                onMouseDown={(e) => {
                  e.preventDefault();
                  setQuery(currency.code);
                  commit(currency.code);
                }}
              >
                {currency.code} — {currency.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
