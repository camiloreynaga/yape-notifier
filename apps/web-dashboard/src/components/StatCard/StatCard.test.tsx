/**
 * Tests for StatCard component
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import StatCard from './StatCard';
import { Bell } from 'lucide-react';

describe('StatCard', () => {
  it('renders with title and value', () => {
    render(
      <StatCard
        title="Test Title"
        value="100"
        icon={Bell}
      />
    );

    expect(screen.getByText('Test Title')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    render(
      <StatCard
        title="Test Title"
        value="100"
        icon={Bell}
        loading={true}
      />
    );

    // Should show skeleton loader (loading state doesn't show title)
    const skeleton = document.querySelector('.animate-pulse');
    expect(skeleton).toBeInTheDocument();
    // Title should not be visible in loading state
    expect(screen.queryByText('Test Title')).not.toBeInTheDocument();
  });

  it('renders trend information when provided', () => {
    render(
      <StatCard
        title="Test Title"
        value="100"
        icon={Bell}
        trend={{
          value: 10,
          label: 'vs last month',
          isPositive: true,
        }}
      />
    );

    expect(screen.getByText(/10%/)).toBeInTheDocument();
    expect(screen.getByText('vs last month')).toBeInTheDocument();
  });

  it('renders link when provided', () => {
    render(
      <StatCard
        title="Test Title"
        value="100"
        icon={Bell}
        link={{
          href: '/test',
          label: 'Ver más',
        }}
      />
    );

    const link = screen.getByText('Ver más →');
    expect(link).toBeInTheDocument();
    expect(link.closest('a')).toHaveAttribute('href', '/test');
  });
});

