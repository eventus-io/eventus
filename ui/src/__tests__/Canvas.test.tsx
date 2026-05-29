import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Canvas } from '../components/Canvas';
import type { Module, RichEvent, CanvasEdge, Layout } from '../types';

const modules: Module[] = [
  { id: 'order', name: 'Order', beanCount: 4, aggregateCount: 1, status: 'HEALTHY' },
];

const richEvents: RichEvent[] = [
  { id: 'com.example.OrderPlaced', simpleName: 'OrderPlaced', publisher: 'order', consumers: [], incomplete: false },
];

const edges: CanvasEdge[] = [
  { kind: 'publish', from: { type: 'module', id: 'order' }, to: { type: 'event', id: 'com.example.OrderPlaced' }, eventId: 'com.example.OrderPlaced' },
];

const layout: Layout = {
  modules: { order: { x: 150, y: 300 } },
  events: { 'com.example.OrderPlaced': { x: 500, y: 300 } },
};

describe('Canvas', () => {
  it('renders module text label', () => {
    render(
      <Canvas
        modules={modules}
        richEvents={richEvents}
        edges={edges}
        layout={layout}
        visibleModuleIds={new Set(['order'])}
        visibleEventIds={new Set(['com.example.OrderPlaced'])}
        selected={null}
        hovered={null}
        setSelected={() => undefined}
        setHovered={() => undefined}
        totalModules={1}
        totalEvents={1}
        incompleteCount={0}
      />,
    );
    expect(screen.getByText('order')).toBeInTheDocument();
  });

  it('renders event simple name label', () => {
    render(
      <Canvas
        modules={modules}
        richEvents={richEvents}
        edges={edges}
        layout={layout}
        visibleModuleIds={new Set(['order'])}
        visibleEventIds={new Set(['com.example.OrderPlaced'])}
        selected={null}
        hovered={null}
        setSelected={() => undefined}
        setHovered={() => undefined}
        totalModules={1}
        totalEvents={1}
        incompleteCount={0}
      />,
    );
    expect(screen.getAllByText('OrderPlaced').length).toBeGreaterThan(0);
  });

  it('shows footer module count', () => {
    render(
      <Canvas
        modules={modules}
        richEvents={richEvents}
        edges={edges}
        layout={layout}
        visibleModuleIds={new Set(['order'])}
        visibleEventIds={new Set(['com.example.OrderPlaced'])}
        selected={null}
        hovered={null}
        setSelected={() => undefined}
        setHovered={() => undefined}
        totalModules={1}
        totalEvents={1}
        incompleteCount={0}
      />,
    );
    expect(screen.getByText(/1 modules/)).toBeInTheDocument();
  });
});
