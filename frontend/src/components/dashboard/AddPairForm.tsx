import { CurrencyPairPicker } from '../pair-picker/CurrencyPairPicker';

interface AddPairFormProps {
  onAdd: (from: string, to: string) => void;
}

export function AddPairForm({ onAdd }: AddPairFormProps) {
  return <CurrencyPairPicker submitLabel="Add pair" onSubmit={onAdd} />;
}
