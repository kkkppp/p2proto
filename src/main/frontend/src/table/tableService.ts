export interface TableDataDto {
    tableLabel: string;
    tableLabelPlural: string;
    tableName: string;
    fieldsToRender: string[];
    allFields: string[];
    columnLabels: Record<string,string>;
    records: Record<string, any>[];
    cellFormatters?: Record<string, 'date' | 'currency' | 'string'>;
}

const API_BASE = process.env.REACT_APP_API_BASE_URL || '/p2proto';

/**
 * Fetch the raw table data for a given tableName and query string.
 * @param tableName  name of the table (e.g. "users")
 * @param query      URL query (e.g. "?page=2&sort=created_at")
 * @returns           a Promise resolving to the raw DTO
 */
export async function fetchTableData(
    tableName: string,
    query: string
): Promise<TableDataDto> {
    const url = `${API_BASE}/table/${encodeURIComponent(tableName)}${query}`;
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) {
        throw new Error(`Failed to fetch table "${tableName}": ${res.status} ${res.statusText}`);
    }
    // will throw if JSON invalid
    return (await res.json()) as TableDataDto;
}