/**
 * Tests for TabBadge component
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import TabBadge from './TabBadge';

describe('TabBadge', () => {
  it('renders nothing when badge is null', () => {
    const { container } = render(<TabBadge badge={null} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when count is 0', () => {
    const { container } = render(
      <TabBadge badge={{ count: 0, variant: 'default' }} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders badge with count', () => {
    render(<TabBadge badge={{ count: 5, variant: 'default' }} />);
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('shows 99+ for counts over 99', () => {
    render(<TabBadge badge={{ count: 150, variant: 'default' }} />);
    expect(screen.getByText('99+')).toBeInTheDocument();
  });

  it('applies correct variant classes', () => {
    const { rerender } = render(
      <TabBadge badge={{ count: 5, variant: 'warning' }} />
    );
    const badge = screen.getByText('5');
    expect(badge).toHaveClass('bg-yellow-500');

    rerender(<TabBadge badge={{ count: 5, variant: 'danger' }} />);
    expect(badge).toHaveClass('bg-red-500');

    rerender(<TabBadge badge={{ count: 5, variant: 'success' }} />);
    expect(badge).toHaveClass('bg-green-500');
  });
});

