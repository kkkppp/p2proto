import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import TablePage from './TablePage';

const contextPath = '/p2proto';

function App() {
    const [menu, setMenu] = useState([]);
    const [activeMenu, setActiveMenu] = useState('');
    const [currentUser, setCurrentUser] = useState(null);

    useEffect(() => {
        fetch(`${contextPath}/api/menu-data`, {
            // Include credentials if needed:
            // credentials: 'include',
        })
            .then((res) => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.status}`);
                }
                return res.json();
            })
            .then((data) => {
                setMenu(data.menu);
                setActiveMenu(data.activeMenu);
                setCurrentUser(data.currentUser);
            })
            .catch((err) => {
                console.error('Error fetching menu data:', err);
            });
    }, []);

    return (
        <div style={{ display: 'flex', height: '100vh', width: '100vw' }}>
            <div style={{ width: '250px' }}>
                <Sidebar
                    menu={menu}
                    activeMenu={activeMenu}
                    currentUser={currentUser}
                    basePath={contextPath}
                />
            </div>
            <div style={{ flex: 1, minWidth: 0, padding: '20px', overflow: 'auto' }}>
                <Routes>
                    <Route path="/table/:tableName" element={<TablePage />} />
                    <Route
                        path="/"
                        element={
                            <div>
                                <h2>Welcome to Platform 2</h2>
                                <p>Your main content here.</p>
                            </div>
                        }
                    />
                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </div>
        </div>
    );
}

export default App;