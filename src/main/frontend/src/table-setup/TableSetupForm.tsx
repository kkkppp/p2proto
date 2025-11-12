// src/table-setup/TableSetupForm.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Card,
    CardContent,
    TextField,
    Button,
    Typography,
    Stack,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { BASE_PATH } from '@/lib/basePath';
import { getCsrfToken } from '@/lib/csrf';

export interface TableSetupDto {
    id?: number | string | null;
    tableName: string;
    tableLabel: string;
    tablePluralLabel: string;
}

interface TableSetupFormProps {
    /** If present → edit mode, otherwise create mode */
    initialTable?: TableSetupDto;
}

export default function TableSetupForm({ initialTable }: TableSetupFormProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();

    const [form, setForm] = useState<TableSetupDto>(() => ({
        id: initialTable?.id ?? null,
        tableName: initialTable?.tableName ?? '',
        tableLabel: initialTable?.tableLabel ?? '',
        tablePluralLabel: initialTable?.tablePluralLabel ?? '',
    }));

    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const isEdit = !!form.id;

    const handleChange =
        (field: keyof TableSetupDto) =>
            (e: React.ChangeEvent<HTMLInputElement>) => {
                setForm((prev) => ({ ...prev, [field]: e.target.value }));
            };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        try {
            setSubmitting(true);

            const token = getCsrfToken();
            const res = await fetch(`${BASE_PATH}/tableSetup/save`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': token ?? '',
                },
                body: JSON.stringify(form),
            });

            if (!res.ok) {
                const text = await res.text().catch(() => '');
                throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
            }

            // on success go back to list
            navigate('/tableSetup');
        } catch (err: any) {
            console.error('Failed to save table setup', err);
            setError(err.message || 'Failed to save');
        } finally {
            setSubmitting(false);
        }
    };

    const handleCancel = () => {
        navigate('/tableSetup');
    };

    return (
        <Card sx={{ maxWidth: 600, mx: 'auto', mt: 3, position: 'relative' }}>
            <CardContent>
                <Typography variant="h5" gutterBottom>
                    {isEdit
                        ? t('tables.editTitle', { defaultValue: 'Edit table' })
                        : t('tables.createTitle', { defaultValue: 'Create table' })}
                </Typography>

                <form onSubmit={handleSubmit} noValidate>
                    {/* Hidden id equivalent */}
                    {form.id != null && form.id !== '' && (
                        <input type="hidden" name="id" value={form.id} />
                    )}

                    <Stack spacing={2} mt={1}>
                        <TextField
                            fullWidth
                            id="tableName"
                            label={t('tables.name')}
                            required
                            value={form.tableName}
                            onChange={handleChange('tableName')}
                            disabled={isEdit} // matches disabled="${not empty table.id}"
                        />

                        <TextField
                            fullWidth
                            id="tableLabel"
                            label={t('tables.label')}
                            required
                            value={form.tableLabel}
                            onChange={handleChange('tableLabel')}
                        />

                        <TextField
                            fullWidth
                            id="tablePluralLabel"
                            label={t('tables.plurallabel')}
                            required
                            value={form.tablePluralLabel}
                            onChange={handleChange('tablePluralLabel')}
                        />

                        {error && (
                            <Typography color="error" variant="body2">
                                {error}
                            </Typography>
                        )}

                        <Stack direction="row" spacing={2} mt={1}>
                            <Button
                                type="submit"
                                variant="contained"
                                disabled={submitting}
                            >
                                {t('button.finish')}
                            </Button>
                            <Button variant="outlined" onClick={handleCancel}>
                                {t('button.cancel')}
                            </Button>
                        </Stack>
                    </Stack>
                </form>

                {submitting && (
                    <Typography sx={{ mt: 2 }} variant="body2">
                        {t('tables.submitting', { defaultValue: 'Submitting…' })}
                    </Typography>
                )}
            </CardContent>
        </Card>
    );
}
