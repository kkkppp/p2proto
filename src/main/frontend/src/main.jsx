import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider, CssBaseline, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './i18n';
import './global.css';

const theme = createTheme({
    palette: {
        primary: { main: '#1976d2' },
        secondary: { main: '#9c27b0' },
    },
});

// 1️⃣ Create a single client instance
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            // for example:
            retry: 1,
            staleTime: 30_000,
        }
    }
});

const container = document.getElementById('root');
const root = createRoot(container);

// 2️⃣ Wrap your app in the provider, passing the instance
root.render(
    <QueryClientProvider client={queryClient}>
        <BrowserRouter basename="/p2proto">
            <ThemeProvider theme={theme}>
                <CssBaseline />
                <App />
            </ThemeProvider>
        </BrowserRouter>
    </QueryClientProvider>
);