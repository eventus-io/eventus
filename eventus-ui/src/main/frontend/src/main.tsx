import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import './tokens.css';

const style = document.createElement('style');
style.textContent = '*, *::before, *::after { box-sizing: border-box; } html, body, #root { margin: 0; padding: 0; height: 100%; } body { font-family: var(--eventus-font-sans); background: var(--eventus-bg); color: var(--eventus-fg); -webkit-font-smoothing: antialiased; overflow: hidden; }';
document.head.appendChild(style);

const root = document.getElementById('root');
if (!root) throw new Error('no #root element');

ReactDOM.createRoot(root).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
