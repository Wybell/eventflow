import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import 'antd/dist/reset.css';
import { App } from './app/App';
import { queryClient } from './shared/lib/query-client';
import './shared/styles/global.css';

const rootElement = document.getElementById('root');

if (rootElement === null) {
  throw new Error('EventFlow application root was not found.');
}

createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
