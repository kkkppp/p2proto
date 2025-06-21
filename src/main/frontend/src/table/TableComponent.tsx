import React from 'react';
import { Link } from 'react-router-dom';
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
    IconButton,
    Stack,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';

/**
 * Generic listing table that mirrors the old JSP table but uses Material‑UI.
 */
export default function TableComponent({
                                           tableLabelPlural = '',
                                           tableLabel = '',
                                           tableName = '',
                                           fieldsToRender = [],
                                           allFields = [],
                                           columnLabels = {},
                                           records = [],
                                           cellFormatters = {},
                                           deleteRecord,
                                       }) {
    const renderFields = fieldsToRender?.length ? fieldsToRender : allFields;

    const formatCell = (field, value) => {
        const fmt = cellFormatters[field];
        if (fmt) return value == null || value === '' ? '' : fmt(value);
        return value == null ? '' : String(value);
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
                    <Typography variant="h5">{tableLabelPlural}</Typography>
                    <Button
                        variant="contained"
                        component={Link}
                        to={`/table/${tableName}/create`}
                    >
                        Create New {tableLabel}
                    </Button>
                </Stack>

                <Table size="small">
                    <TableHead>
                        <TableRow>
                            {renderFields.map((field) => (
                                <TableCell key={field}>{columnLabels[field]}</TableCell>
                            ))}
                            <TableCell width={120}>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {records.map((rec) => (
                            <TableRow key={rec.id} hover>
                                {renderFields.map((field) => (
                                    <TableCell key={field}>{formatCell(field, rec[field])}</TableCell>
                                ))}
                                <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                    <IconButton
                                        component={Link}
                                        to={`/table/${tableName}/${rec.id}/edit`}
                                        size="small"
                                        color="primary"
                                    >
                                        <EditIcon fontSize="inherit" />
                                    </IconButton>
                                    <IconButton
                                        size="small"
                                        color="error"
                                        onClick={() => deleteRecord(tableName, rec.id)}
                                    >
                                        <DeleteIcon fontSize="inherit" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    );
}
