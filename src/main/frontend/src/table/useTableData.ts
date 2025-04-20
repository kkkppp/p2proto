// src/hooks/useTableData.ts
import { useQuery } from '@tanstack/react-query';
import { fetchTableData, TableDataDto } from './tableService';

export function useTableData(tableName: string, query: string) {
    return useQuery<TableDataDto, Error>({
        queryKey: ['tableData', tableName, query],
        queryFn: () => fetchTableData(tableName, query),
        // per‑query options still work here:
        staleTime: 30_000,
        retry: 2,
    });
}
