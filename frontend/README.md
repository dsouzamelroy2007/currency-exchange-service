# Currency Exchange Frontend

React + Vite + TypeScript SPA for the currency-exchange-service backend. See [../plan.md](../plan.md)
for the original build plan and stage-by-stage decisions locked in during initial implementation.

## Commands

```bash
npm install
npm run dev      # dev server on :5173, proxying /btc to the backend on :8080
npm run build    # typecheck + production build
npm run test     # Vitest + React Testing Library
```

Run `mvn spring-boot:run` in the repo root alongside `npm run dev` for a working local setup.

## Decisions

- **Currency symbols: static lookup table, code as fallback.** `knownCurrencies.ts` now carries a
  `symbol` field per entry (`$`, `€`, `₿`, `Ξ`, ...) alongside `code`/`name`, with
  `currencySymbol(code)` falling back to the uppercased code itself for anything not in the static
  list (free-typed codes are still fully supported elsewhere in the app; this just means an unknown
  one shows as text instead of a glyph). Used in `PairCard` (next to the pair codes and before the
  rate value) and in the `CurrencyCombobox` dropdown.
- **Per-pair accent color: deterministic hash of the pair key, not user-assigned.** `pairAccentColor`
  (in `utils/currencyPair.ts`) hashes `FROM_TO` into an HSL hue at fixed saturation/lightness, so
  the same pair always gets the same color across sessions with zero state to manage, and cards
  read as visually distinct instead of a uniform gray list. It's exposed as the `--pair-accent` CSS
  custom property on `.pair-card` and used for the left border, the currency symbols, and the rate
  text.
- **Global palette: CSS custom properties on `:root`, with a `prefers-color-scheme: dark` override.**
  `global.css` defines `--color-bg`/`--color-surface`/`--color-primary`/etc. once and every
  component styles off those tokens rather than hardcoded colors, so the light/dark swap is a single
  block of variable redefinitions. No manual theme toggle — this follows the OS/browser preference
  only, matching the `color-scheme: light dark` already declared on `:root`.
