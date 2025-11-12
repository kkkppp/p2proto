// src/table-setup/TableSetupList.jsx
import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    Card,
    CardContent,
    Typography,
    Button,
    Table,
    TableHead,
    TableRow,
    TableCell,
    TableBody,
    Stack,
    IconButton,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import ListAltIcon from '@mui/icons-material/ListAlt';
import DeleteIcon from '@mui/icons-material/Delete';

/**
 * metadataList: Array of {
 *   id,
 *   tableName,
 *   tableLabel,
 *   tablePluralLabel
 * }
 *
 * onDelete?: (id) => void  // optional callback when delete is confirmed
 */
export default function TableSetupList({ metadataList = [], onDelete }) {
    const { t } = useTranslation();

    const handleDelete = (id, tableName) => {
        const label =
            t('tables.name', { defaultValue: 'table' }) + ' ' + (tableName || '');
        const msg =
            t('tables.confirmDelete', {
                defaultValue: 'Are you sure you want to delete {{item}}?',
                item: label,
            }) || `Are you sure you want to delete ${label}?`;

        if (window.confirm(msg)) {
            onDelete && onDelete(id);
        }
    };

    return (
        <Card sx={{ mt: 3 }}>
            <CardContent>
                <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                    sx={{ mb: 2 }}
                >
                    <Typography variant="h5">
                        {t('tables.title', { defaultValue: 'Tables' })}
                    </Typography>

                    <Button
                        variant="contained"
                        component={RouterLink}
                        to="/tableSetup/create"
                    >
                        {t('tables.button.create', {
                            defaultValue: 'Create table',
                        })}
                    </Button>
                </Stack>

                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>
                                {t('tables.name', { defaultValue: 'Name' })}
                            </TableCell>
                            <TableCell>
                                {t('tables.label', { defaultValue: 'Label' })}
                            </TableCell>
                            <TableCell>
                                {t('tables.plurallabel', { defaultValue: 'Plural label' })}
                            </TableCell>
                            <TableCell width={180}>
                                {t('label.actions', { defaultValue: 'Actions' })}
                            </TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {metadataList.map((table) => (
                            <TableRow key={table.id} hover>
                                <TableCell>{table.tableName}</TableCell>
                                <TableCell>{table.tableLabel}</TableCell>
                                <TableCell>{table.tablePluralLabel}</TableCell>
                                <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                    {/* Edit */}
                                    <IconButton
                                        component={RouterLink}
                                        to={`/tableSetup/${table.id}/edit`}
                                        size="small"
                                        color="primary"
                                        sx={{ mr: 1 }}
                                    >
                                        <EditIcon fontSize="inherit" />
                                    </IconButton>

                                    {/* Fields */}
                                    <IconButton
                                        component={RouterLink}
                                        to={`/tableSetup/${table.id}/fields`}
                                        size="small"
                                        color="secondary"
                                        sx={{ mr: 1 }}
                                    >
                                        <ListAltIcon fontSize="inherit" />
                                    </IconButton>

                                    {/* Delete */}
                                    <IconButton
                                        size="small"
                                        color="error"
                                        onClick={() =>
                                            handleDelete(table.id, table.tableName)
                                        }
                                    >
                                        <DeleteIcon fontSize="inherit" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}

                        {metadataList.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={4}>
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                        align="center"
                                    >
                                        {t('tables.empty', {
                                            defaultValue: 'No tables defined yet.',
                                        })}
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    );
}
