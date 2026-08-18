export interface KnownCurrency {
  code: string;
  name: string;
  symbol: string;
}

export const KNOWN_CURRENCIES: KnownCurrency[] = [
  { code: 'USD', name: 'US Dollar', symbol: '$' },
  { code: 'EUR', name: 'Euro', symbol: '€' },
  { code: 'GBP', name: 'British Pound', symbol: '£' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥' },
  { code: 'CHF', name: 'Swiss Franc', symbol: 'CHF' },
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'CA$' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' },
  { code: 'NZD', name: 'New Zealand Dollar', symbol: 'NZ$' },
  { code: 'CNY', name: 'Chinese Yuan', symbol: '¥' },
  { code: 'HKD', name: 'Hong Kong Dollar', symbol: 'HK$' },
  { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$' },
  { code: 'INR', name: 'Indian Rupee', symbol: '₹' },
  { code: 'BRL', name: 'Brazilian Real', symbol: 'R$' },
  { code: 'ZAR', name: 'South African Rand', symbol: 'R' },
  { code: 'SEK', name: 'Swedish Krona', symbol: 'kr' },
  { code: 'NOK', name: 'Norwegian Krone', symbol: 'kr' },
  { code: 'MXN', name: 'Mexican Peso', symbol: 'MX$' },
  { code: 'KRW', name: 'South Korean Won', symbol: '₩' },
  { code: 'TRY', name: 'Turkish Lira', symbol: '₺' },
  { code: 'AED', name: 'UAE Dirham', symbol: 'د.إ' },
  { code: 'BTC', name: 'Bitcoin', symbol: '₿' },
  { code: 'ETH', name: 'Ethereum', symbol: 'Ξ' },
  { code: 'USDT', name: 'Tether', symbol: '₮' },
  { code: 'USDC', name: 'USD Coin', symbol: 'USDC' },
  { code: 'BNB', name: 'Binance Coin', symbol: 'BNB' },
  { code: 'XRP', name: 'Ripple', symbol: 'XRP' },
  { code: 'SOL', name: 'Solana', symbol: 'SOL' },
  { code: 'DOGE', name: 'Dogecoin', symbol: 'Ð' },
  { code: 'ADA', name: 'Cardano', symbol: 'ADA' },
  { code: 'LTC', name: 'Litecoin', symbol: 'Ł' },
];

const SYMBOL_BY_CODE = new Map(KNOWN_CURRENCIES.map((currency) => [currency.code, currency.symbol]));

export function currencySymbol(code: string): string {
  return SYMBOL_BY_CODE.get(code.toUpperCase()) ?? code.toUpperCase();
}

export function filterCurrencies(query: string, list: KnownCurrency[]): KnownCurrency[] {
  const trimmed = query.trim().toLowerCase();
  if (!trimmed) return list;
  return list.filter(
    (currency) =>
      currency.code.toLowerCase().includes(trimmed) || currency.name.toLowerCase().includes(trimmed),
  );
}
