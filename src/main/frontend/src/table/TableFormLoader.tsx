import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { BASE_PATH } from '@/lib/basePath';
import { getCsrfToken } from '@/lib/csrf';
import TableForm from './TableForm';

/**
 * Fetches form metadata (and existing values when editing) then renders TableForm.
 * Adds credentials so the XSRF‑TOKEN cookie is set on the very first request, and
 * forwards the fresh token down to TableForm via the csrfToken prop.
 */
export default function TableFormLoader() {
    const { tableName, recordId } = useParams();
    const [payload, setPayload] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                const url = recordId
                    ? `${BASE_PATH}/table/${tableName}/${recordId}/edit`
                    : `${BASE_PATH}/table/${tableName}/create`;

                const res = await fetch(url, {
                    credentials: 'include',   // ① send cookies so Spring sets XSRF‑TOKEN
                    signal: controller.signal,
                    headers: { Accept: 'application/json' },
                });

                if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
                const data = await res.json();

                // ② attach the freshly‑set XSRF cookie value so the form can POST safely
                setPayload({ ...data, csrfToken: getCsrfToken() });
            } catch (err) {
                if (err.name !== 'AbortError') {
                    console.error(err);
                    setError(err);
                }
            }
        }

        load();
        return () => controller.abort();
    }, [tableName, recordId]);

    if (error) return <p style={{ color: 'red' }}>Failed to load form.</p>;
    if (!payload) return <p>Loading…</p>;

    return <TableForm {...payload} />;
}
