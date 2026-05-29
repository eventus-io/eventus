import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { App } from '../App';
import type { GraphData } from '../types';

const GRAPH: GraphData = {
  modules: [
    { id: 'order', name: 'Order', beanCount: 4, aggregateCount: 1, status: 'HEALTHY' },
    { id: 'payments', name: 'Payments', beanCount: 5, aggregateCount: 2, status: 'WARNING' },
  ],
  events: [
    { id: 'com.example.OrderPlaced', name: 'OrderPlaced', publisherModuleId: 'order' },
  ],
  edges: [
    { id: 'e1', eventId: 'com.example.OrderPlaced', fromModuleId: 'order', toModuleId: null, edgeType: 'PUBLISHES' },
    { id: 'e2', eventId: 'com.example.OrderPlaced', fromModuleId: null, toModuleId: 'payments', edgeType: 'LISTENS_TO' },
  ],
  publications: [],
};

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => {
    if (url.includes('/violations')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    if (url.includes('/drift')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ drifts: [], totalDrifts: 0, breachingCount: 0, comparedAt: 0 }) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve(GRAPH) });
  }));
});

describe('App', () => {
  it('renders the top bar with eventus lockup', () => {
    render(<App />);
    expect(screen.getAllByText('eventus').length).toBeGreaterThan(0);
  });

  it('shows module names in sidebar after data loads', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getAllByText('order').length).toBeGreaterThan(0);
    });
    expect(screen.getAllByText('payments').length).toBeGreaterThan(0);
  });

  it('shows event name in sidebar after data loads', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getAllByText('OrderPlaced').length).toBeGreaterThan(0);
    });
  });

  it('renders inspector empty state initially', () => {
    render(<App />);
    expect(screen.getByText('NO SELECTION')).toBeInTheDocument();
  });
});
