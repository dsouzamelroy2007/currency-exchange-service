import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { CurrencyCombobox } from '../../src/components/pair-picker/CurrencyCombobox';

describe('CurrencyCombobox', () => {
  it('filters the dropdown list as the user types', async () => {
    const user = userEvent.setup();
    render(<CurrencyCombobox label="From" value="" onChange={vi.fn()} />);

    const input = screen.getByRole('combobox');
    await user.click(input);
    expect(screen.getByText(/Euro/)).toBeInTheDocument();

    await user.type(input, 'bitcoin');

    expect(screen.getByText(/Bitcoin/)).toBeInTheDocument();
    expect(screen.queryByText(/Euro/)).not.toBeInTheDocument();
  });

  it('calls onChange with the selected code when an option is clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<CurrencyCombobox label="From" value="" onChange={onChange} />);

    const input = screen.getByRole('combobox');
    await user.click(input);
    await user.type(input, 'euro');
    await user.click(screen.getByText(/Euro/));

    expect(onChange).toHaveBeenCalledWith('EUR');
  });

  it('still accepts an unlisted typed value on blur', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <div>
        <CurrencyCombobox label="From" value="" onChange={onChange} />
        <button type="button">elsewhere</button>
      </div>,
    );

    const input = screen.getByRole('combobox');
    await user.click(input);
    await user.type(input, 'xau');
    await user.click(screen.getByText('elsewhere'));

    expect(onChange).toHaveBeenCalledWith('XAU');
  });
});
