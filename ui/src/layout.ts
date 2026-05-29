import type { Module, RichEvent, Layout } from './types';

const VB_W = 1000;
const VB_H = 600;
const PAD = 80;

export function computeLayout(modules: Module[], events: RichEvent[]): Layout {
  const mLayout: Record<string, { x: number; y: number }> = {};
  const eLayout: Record<string, { x: number; y: number }> = {};

  const mCount = modules.length;
  const eCount = events.length;
  const usable = VB_H - PAD * 2;

  modules.forEach((m, i) => {
    mLayout[m.id] = {
      x: 150,
      y: PAD + (mCount <= 1 ? usable / 2 : (i / (mCount - 1)) * usable),
    };
  });

  events.forEach((e, i) => {
    eLayout[e.id] = {
      x: VB_W / 2,
      y: PAD + (eCount <= 1 ? usable / 2 : (i / (eCount - 1)) * usable),
    };
  });

  return { modules: mLayout, events: eLayout };
}
