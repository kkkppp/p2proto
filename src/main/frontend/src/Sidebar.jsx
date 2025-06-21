import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';
import {
    Drawer,
    List,
    ListItemButton,
    ListItemText,
    Collapse,
    Toolbar,
    Typography,
    Box,
    Divider,
    Button,
    Stack,
} from '@mui/material';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';

const drawerWidth = 240;

/**
 * Permanent sidebar that matches the MUI look used elsewhere.
 *
 * Props:
 *  - menu: [{ title, url?, children?: [...] }]
 *  - activeMenu: string | null
 *  - currentUser: { fullName }
 *  - basePath: context path (e.g. "/p2proto")
 */
export default function Sidebar({
                                    menu = [],
                                    activeMenu: initialActiveMenu,
                                    currentUser,
                                    basePath,
                                }) {
    const { t } = useTranslation();
    const [openMenu, setOpenMenu] = useState(initialActiveMenu || null);

    const toggle = (title, hasChildren) => {
        if (!hasChildren) return;
        setOpenMenu((prev) => (prev === title ? null : title));
    };

    return (
        <Drawer
            variant="permanent"
            sx={{
                width: drawerWidth,
                flexShrink: 0,
                [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
            }}
        >
            {/* logo / title */}
            <Toolbar sx={{ bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                <Typography variant="h6" noWrap>
                    Platform 2
                </Typography>
            </Toolbar>

            {/* main nav list */}
            <List>
                {menu.map((item) => {
                    const hasChildren = !!item.children?.length;
                    const isOpen = openMenu === item.title;
                    return (
                        <Box key={item.title}>
                            <ListItemButton onClick={() => toggle(item.title, hasChildren)}>
                                <ListItemText primary={item.title} />
                                {hasChildren ? isOpen ? <ExpandLess /> : <ExpandMore /> : null}
                            </ListItemButton>

                            {hasChildren && (
                                <Collapse in={isOpen} timeout="auto" unmountOnExit>
                                    <List component="div" disablePadding>
                                        {item.children.map((sub) => (
                                            <ListItemButton
                                                key={sub.title}
                                                component={RouterLink}
                                                to={sub.url}
                                                sx={{ pl: 4 }}
                                            >
                                                <ListItemText primary={sub.title} />
                                            </ListItemButton>
                                        ))}
                                    </List>
                                </Collapse>
                            )}
                        </Box>
                    );
                })}
            </List>

            <Divider sx={{ mt: 'auto' }} />

            {/* footer / logout */}
            <Box sx={{ p: 2 }}>
                {currentUser?.fullName && (
                    <Typography variant="body2" color="text.secondary" mb={1}>
                        {t('current.user', { user: currentUser.fullName })}
                    </Typography>
                )}

                <Stack direction="row" justifyContent="flex-start">
                    <Button
                        size="small"
                        variant="outlined"
                        component="a"
                        href={`${basePath}/logout`}
                    >
                        {t('logout')}
                    </Button>
                </Stack>
            </Box>
        </Drawer>
    );
}
