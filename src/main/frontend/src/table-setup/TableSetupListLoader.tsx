// src/table-setup/TableSetupListLoader.tsx
import { useEffect, useState } from 'react';
import { BASE_PATH } from '@/lib/basePath';
import TableSetupList from './TableSetupList';

export default function TableSetupListLoader() {
    const [metadataList, setMetadataList] = useState([]);
    const [error, setError] = useState<null | string>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                setLoading(true);
                const res = await fetch(`${BASE_PATH}/tableSetup/api`, { credentials: 'include' });
                if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
                const data = await res.json();
                setMetadataList(data);
            } catch (err: any) {
                if (err.name !== 'AbortError') {
                    console.error(err);
                    setError(err.message || 'Failed to load tables');
                }
            } finally {
                setLoading(false);
            }
        }

        load();
        return () => controller.abort();
    }, []);

    if (loading) return <p>Loading…</p>;
    if (error) return <p style={{ color: 'red' }}>{error}</p>;

    return <TableSetupList metadataList={metadataList} />;
}
