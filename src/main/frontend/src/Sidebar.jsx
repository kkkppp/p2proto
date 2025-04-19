import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import './sidebar.css';

function Sidebar({ menu = [], activeMenu: initialActiveMenu, currentUser, basePath }) {
    const { t } = useTranslation(); // initialize the translation function
    // Use local state to track the active (open) menu.
    const [activeMenu, setActiveMenu] = useState(initialActiveMenu || null);

    const handleToggle = (menuTitle) => {
        // Toggle between the same menu closing and new one opening.
        setActiveMenu(activeMenu === menuTitle ? null : menuTitle);
    };

    return (
        <div className="sidebar">
            <h1>Platform 2</h1>
            <ul>
                {menu.map((menuItem) => {
                    const hasChildren = menuItem.children && menuItem.children.length > 0;
                    const isOpen = menuItem.title === activeMenu;
                    const liClass = isOpen ? 'open' : '';

                    return (
                        <li key={menuItem.title} className={liClass}>
                            <a
                                href="#"
                                className={hasChildren ? 'has-submenu' : ''}
                                onClick={(e) => {
                                    e.preventDefault();
                                    if (hasChildren) {
                                        handleToggle(menuItem.title);
                                    }
                                }}
                            >
                                {menuItem.title}
                            </a>
                            {hasChildren && isOpen && (
                                <ul className="subMenu">
                                    {menuItem.children.map((sub) => (
                                        <li key={sub.title}>
                                            <Link to={sub.url} data-url={`${basePath}${sub.url}`}>
                                                {sub.title}
                                            </Link>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </li>
                    );
                })}
            </ul>

            {/* Logout Section */}
            <div className="logout-container">
                {currentUser && currentUser.fullName && (
                    <>
                        {/* Use the resource bundle text with interpolation to insert current user data */}
                        <span>{t('current.user', { user: currentUser.fullName })}</span>
                        <br />
                    </>
                )}
                <a href={`${basePath}/logout`} className="logout-link">
                    {t('logout')}
                </a>
            </div>
        </div>
    );
}

export default Sidebar;
