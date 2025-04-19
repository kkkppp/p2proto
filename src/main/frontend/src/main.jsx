import React from 'react';
import { createRoot } from 'react-dom/client'; // Import createRoot from react-dom/client
import { BrowserRouter } from 'react-router-dom';
import App from './App'; // Assume App contains your Sidebar component or routes
import './i18n';
import './global.css';

const container = document.getElementById('root');
const root = createRoot(container);

root.render(
    <BrowserRouter basename="/p2proto">
        <App />
    </BrowserRouter>
);