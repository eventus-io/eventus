import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Inspector } from '../components/Inspector';
import type { Module, RichEvent } from '../types';

const modules: Module[] = [
  { id: 'order', name: 'Order', beanCount: 4, aggregateCount: 1, status: 'HEALTHY' },
  { id: 'payments', name: 'Payments', beanCount: 2, aggregateCount: 0, status: 'WARNING' },
];

const richEvents: RichEvent[] = [
  { id: 'com.example.OrderPlaced', simpleName: 'OrderPlaced', publisher: 'order', consumers: ['payments'], incomplete: false },
];

describe('Inspector — no selection', () => {
  it('shows NO SELECTION placeholder', () => {
    render(
      <Inspector
        selected={null}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText('NO SELECTION')).toBeInTheDocument();
  });
});

describe('Inspector — module selected', () => {
  it('shows module eyebrow and name', () => {
    render(
      <Inspector
        selected={{ type: 'module', id: 'order' }}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText('MODULE')).toBeInTheDocument();
    expect(screen.getAllByText('order').length).toBeGreaterThan(0);
  });

  it('shows published events section', () => {
    render(
      <Inspector
        selected={{ type: 'module', id: 'order' }}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText(/PUBLISHES/)).toBeInTheDocument();
    expect(screen.getByText('OrderPlaced')).toBeInTheDocument();
  });

  it('calls setSelected(null) when esc is clicked', async () => {
    const setSelected = vi.fn();
    render(
      <Inspector
        selected={{ type: 'module', id: 'order' }}
        setSelected={setSelected}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    await userEvent.click(screen.getByText('esc'));
    expect(setSelected).toHaveBeenCalledWith(null);
  });
});

describe('Inspector — event selected', () => {
  it('shows event name and FQN', () => {
    render(
      <Inspector
        selected={{ type: 'event', id: 'com.example.OrderPlaced' }}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText('OrderPlaced')).toBeInTheDocument();
    expect(screen.getByText('com.example.OrderPlaced')).toBeInTheDocument();
  });

  it('shows publisher module chip', () => {
    render(
      <Inspector
        selected={{ type: 'event', id: 'com.example.OrderPlaced' }}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText('PUBLISHED BY')).toBeInTheDocument();
  });

  it('shows consumer chips', () => {
    render(
      <Inspector
        selected={{ type: 'event', id: 'com.example.OrderPlaced' }}
        setSelected={() => undefined}
        modules={modules}
        richEvents={richEvents}
      />,
    );
    expect(screen.getByText('payments')).toBeInTheDocument();
  });
});
