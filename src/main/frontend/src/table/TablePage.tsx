import React, { useMemo } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import TableComponent from './TableComponent';
import { useTableData } from './useTableData';

export default function TablePage() {
    const { tableName } = useParams<{ tableName: string }>();
    const location = useLocation();
    const query = location.search;                // e.g. "?page=2"

    // 🪝 all your fetch / loading / error / cache logic lives in the hook now
    const {
        data: tableData,
        isLoading,
        isError,
        error,
    } = useTableData(tableName!, query);

    // derive the actual formatter functions from whatever the API told us
    const cellFormatters = useMemo(() => {
        if (!tableData?.cellFormatters) return {};
        return Object.entries(tableData.cellFormatters).reduce<
            Record<string, (val: any) => string>
        >((acc, [field, kind]) => {
            if (kind === 'date') {
                acc[field] = (ms) => new Date(ms).toLocaleString();
            }
            // add more kinds here as needed...
            return acc;
        }, {});
    }, [tableData]);

    // callbacks still come from the page/container layer
    const loadContent = (e: React.MouseEvent, url: string) => {
        e.preventDefault();
        console.log('Load content from:', url);
        // your dynamic navigation / modal logic
    };
    const deleteRecord = (table: string, id: string) => {
        console.log(`Deleting record ${id} from ${table}`);
        // your deletion logic
    };

    if (isLoading) return <div>Loading…</div>;
    if (isError)   return <div>Error: {(error as Error).message}</div>;
    if (!tableData) return <div>No data for “{tableName}”</div>;

    return (
        <TableComponent
            {...tableData}
            cellFormatters={cellFormatters}
            loadContent={loadContent}
            deleteRecord={deleteRecord}
        />
    );
}