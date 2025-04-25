import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import TableForm from './TableForm';

export default function TableFormLoader() {
    const { tableName, recordId } = useParams();
    const [payload, setPayload] = useState(null);

    useEffect(() => {
        const url = recordId
            ? `/table/${tableName}/${recordId}/edit`
            : `/table/${tableName}/create`;
        fetch(url)
            .then(r => r.json())
            .then(setPayload)
            .catch(console.error);
    }, [tableName, recordId]);

    if (!payload) return <p>Loading…</p>;

    return <TableForm {...payload} />;
}