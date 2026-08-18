export interface KnownCurrency {
  code: string;
  name: string;
}

export const KNOWN_CURRENCIES: KnownCurrency[] = [
  { code: 'USD', name: 'US Dollar' },
  { code: 'EUR', name: 'Euro' },
  { code: 'GBP', name: 'British Pound' },
  { code: 'JPY', name: 'Japanese Yen' },
  { code: 'CHF', name: 'Swiss Franc' },
  { code: 'CAD', name: 'Canadian Dollar' },
  { code: 'AUD', name: 'Australian Dollar' },
  { code: 'NZD', name: 'New Zealand Dollar' },
  { code: 'CNY', name: 'Chinese Yuan' },
  { code: 'HKD', name: 'Hong Kong Dollar' },
  { code: 'SGD', name: 'Singapore Dollar' },
  { code: 'INR', name: 'Indian Rupee' },
  { code: 'BRL', name: 'Brazilian Real' },
  { code: 'ZAR', name: 'South African Rand' },
  { code: 'SEK', name: 'Swedish Krona' },
  { code: 'NOK', name: 'Norwegian Krone' },
  { code: 'MXN', name: 'Mexican Peso' },
  { code: 'KRW', name: 'South Korean Won' },
  { code: 'TRY', name: 'Turkish Lira' },
  { code: 'AED', name: 'UAE Dirham' },
  { code: 'BTC', name: 'Bitcoin' },
  { code: 'ETH', name: 'Ethereum' },
  { code: 'USDT', name: 'Tether' },
  { code: 'USDC', name: 'USD Coin' },
  { code: 'BNB', name: 'Binance Coin' },
  { code: 'XRP', name: 'Ripple' },
  { code: 'SOL', name: 'Solana' },
  { code: 'DOGE', name: 'Dogecoin' },
  { code: 'ADA', name: 'Cardano' },
  { code: 'LTC', name: 'Litecoin' },
];

export function filterCurrencies(query: string, list: KnownCurrency[]): KnownCurrency[] {
  const trimmed = query.trim().toLowerCase();
  if (!trimmed) return list;
  return list.filter(
    (currency) =>
      currency.code.toLowerCase().includes(trimmed) || currency.name.toLowerCase().includes(trimmed),
  );
}
