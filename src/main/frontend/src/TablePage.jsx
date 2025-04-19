import React, {useEffect, useMemo, useState} from 'react';
import { useParams, useLocation } from 'react-router-dom';
import TableComponent from './TableComponent';

const contextPath = '/p2proto';

function TablePage() {
    const { tableName } = useParams();
    const location = useLocation(); // Get the current URL details
    const [tableData, setTableData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Build the fetch URL including any query parameters present in the URL.
    const fetchUrl = `${contextPath}/table/${tableName}${location.search}`;

    useEffect(() => {
        fetch(fetchUrl)
            .then((res) => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.status}`);
                }
                return res.json();
            })
            .then((data) => {
                console.log('Fetched Table Data:', data);
                setTableData(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error('Error fetching table data:', err);
                setError(err);
                setLoading(false);
            });
    }, [fetchUrl]);

    const cellFormatters = useMemo(() => {
        if (!tableData || !tableData.cellFormatters) return {};
        return Object.entries(tableData.cellFormatters).reduce((acc, [field, kind]) => {
            if (kind === 'date') {
                acc[field] = (ms) => new Date(ms).toLocaleString();
            }
            // other kinds can be added here
            return acc;
        }, {});
    }, [tableData]);

    const loadContent = (event, url) => {
        event.preventDefault();
        // Custom logic for dynamic content loading (navigation, modal, etc.)
        console.log("Load content from:", url);
    };

    const deleteRecord = (tableName, recordId) => {
        // Custom logic for record deletion
        console.log(`Deleting record ${recordId} from ${tableName}`);
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error.message}</div>;
    if (!tableData) return <div>No data found for table: {tableName}</div>;

    return (
        <TableComponent
            {...tableData}
            cellFormatters={cellFormatters}
            loadContent={loadContent}
            deleteRecord={deleteRecord}
        />
    );
}

export default TablePage;