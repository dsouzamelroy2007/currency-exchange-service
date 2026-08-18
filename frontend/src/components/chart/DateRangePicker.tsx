import { useState } from 'react';
import { daysAgoIsoDate, todayIsoDate } from '../../utils/dateRange';

interface DateRangePickerProps {
  onApply: (startDate: string, endDate: string) => void;
}

export function DateRangePicker({ onApply }: DateRangePickerProps) {
  const [startDate, setStartDate] = useState(daysAgoIsoDate(7));
  const [endDate, setEndDate] = useState(todayIsoDate());

  const apply = (e: React.FormEvent) => {
    e.preventDefault();
    onApply(startDate, endDate);
  };

  return (
    <form className="date-range-picker" onSubmit={apply}>
      <label>
        Start date
        <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
      </label>
      <label>
        End date
        <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
      </label>
      <button type="submit">Apply</button>
    </form>
  );
}
