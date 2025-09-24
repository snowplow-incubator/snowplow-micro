import type { ColumnDef } from '@tanstack/react-table';
import { Button } from '@/components/ui/button';
import { ArrowUp, ArrowDown, Search } from 'lucide-react';
import type { Event } from '@/services/api';
import { DraggableColumn } from '@/components/DraggableColumn';
import { TruncatedCell } from '@/components/TruncatedCell';
import { TruncatedColumnName } from '@/components/TruncatedColumnName';

export function truncateColumnName(columnName: string, limit: number = 35): string {
  let result = columnName;

  // Replace contexts_ with c[...]_
  if (result.startsWith('contexts_')) {
    result = result.replace('contexts_', 'c[...]_');
  }

  // Replace unstruct_event_ with ue[...]_
  if (result.startsWith('unstruct_event_')) {
    result = result.replace('unstruct_event_', 'ue[...]_');
  }

  // If still over limit chars, truncate in the middle
  if (result.length > limit) {
    const totalLength = limit;
    const ellipsis = '[...]';
    const availableLength = totalLength - ellipsis.length;
    const startLength = Math.ceil(availableLength / 2);
    const endLength = Math.floor(availableLength / 2);

    const start = result.substring(0, startLength);
    const end = result.substring(result.length - endLength);

    result = `${start}${ellipsis}${end}`;
  }

  return result;
}

export function getColumnInfo(columnName: string, limit: number = 35) {
  let result = columnName;
  let icon: 'contexts' | 'unstruct' | null = null;

  // Check for contexts_ prefix
  if (result.startsWith('contexts_')) {
    result = result.replace('contexts_', '');
    icon = 'contexts';
  }

  // Check for unstruct_event_ prefix
  if (result.startsWith('unstruct_event_')) {
    result = result.replace('unstruct_event_', '');
    icon = 'unstruct';
  }

  // If still over limit chars, truncate in the middle
  if (result.length > limit) {
    const totalLength = limit;
    const ellipsis = '[...]';
    const availableLength = totalLength - ellipsis.length;
    const startLength = Math.ceil(availableLength / 2);
    const endLength = Math.floor(availableLength / 2);

    const start = result.substring(0, startLength);
    const end = result.substring(result.length - endLength);

    result = `${start}${ellipsis}${end}`;
  }

  return { text: result, icon, original: columnName };
}

/**
 * Get a summary for JSON values (objects/arrays)
 */
function getJsonSummary(value: any): string {
  if (Array.isArray(value)) {
    const count = value.length;
    return `${count} ${count === 1 ? 'entity' : 'entities'}`;
  } else if (typeof value === 'object' && value !== null) {
    const keys = Object.keys(value);
    const count = keys.length;
    return `${count} ${count === 1 ? 'field' : 'fields'}`;
  }
  return 'JSON';
}

const isJSON = (fieldName: string) =>
  fieldName.startsWith("contexts_") ||
  fieldName.startsWith("unstruct_event_")

const isTimestamp = (fieldName: string) =>
  fieldName.endsWith("_tstamp")

/**
 * Generate columns from selected fields and available field info
 */
export function generateColumns(
  selectedColumns: string[],
  selectedCellId: string | null,
  onJsonCellToggle: (cellId: string, value: any, title: string) => void,
  onReorderColumns: (fromIndex: number, toIndex: number) => void
): ColumnDef<Event>[] {
  const columns: ColumnDef<Event>[] = [];

  // Pinned status column
  columns.push({
    accessorKey: 'contexts_com_snowplowanalytics_snowplow_failure_1',
    header: () => {
      return (
        <div className="flex items-center justify-center">
          <span className="font-normal">Status</span>
        </div>
      );
    },
    cell: ({ getValue, row }) => {
      const value = getValue();
      const cellId = `${row.original.event_id}-status`;
      const isSelected = selectedCellId === cellId;

      // Handle undefined values - show OK status
      if (value === undefined || (Array.isArray(value) && value.length === 0)) {
        return (
          <div className="px-2 py-1 flex justify-center">
            <span className="px-3 py-1 rounded-full text-xs whitespace-nowrap bg-(--light-teal) text-(--very-dark-teal) font-normal">
              Valid
            </span>
          </div>
        );
      }

      if (Array.isArray(value) && onJsonCellToggle) {
        const failureCount = value.length;
        return (
          <div
            className="px-2 py-1 flex items-center justify-center gap-2 cursor-pointer group"
            onClick={() => onJsonCellToggle(cellId, value.map(v => ({data: v.data, errors: v.errors, failureType: v.failureType})), 'Failures')}
            data-cell-clickable="true"
          >
            <span className={`px-2 py-1 rounded-full text-xs font-normal whitespace-nowrap transition-colors ${
              isSelected
                ? 'bg-(--dark-orange) text-(--very-dark-orange)'
                : 'bg-(--light-orange) text-(--very-dark-orange) group-hover:bg-(--dark-orange)'
            }`}>
              {failureCount} {failureCount === 1 ? 'failure' : 'failures'}
            </span>
          </div>
        );
      }

      // Regular formatting for primitive values
      return (
        <div className="px-2 py-1">
          {String(value)}
        </div>
      );
    },
    enableSorting: false,
    enableColumnFilter: false
  });

  // Add selected columns
  selectedColumns.forEach((fieldName, index) => {

    columns.push({
      accessorKey: fieldName,
      id: fieldName,
      header: ({ column }) => {
        return (
          <DraggableColumn
            index={index}
            onReorder={onReorderColumns}
          >
            <div className="flex items-center group">
              <Button
                variant="ghost"
                onClick={() => column.toggleSorting(column.getIsSorted() !== 'desc')}
                className="h-8 px-2 cursor-pointer"
              >
                <TruncatedColumnName columnName={fieldName} />
              </Button>
              <Button
                variant="ghost"
                onClick={() => column.toggleSorting(column.getIsSorted() !== 'desc')}
                className="h-4 w-5 p-0 cursor-pointer"
                title="Sort column"
              >
                {column.getIsSorted() === 'asc' ? (
                  <ArrowUp className="h-4 w-4" />
                ) : column.getIsSorted() === 'desc' ? (
                  <ArrowDown className="h-4 w-4" />
                ) : (
                  <ArrowDown className="h-4 w-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                )}
              </Button>
            </div>
          </DraggableColumn>
        );
      },
      cell: ({ getValue, row }) => {
        const value = getValue();

        // Handle undefined values - show empty cell
        if (value === undefined) {
          return <div className="px-2 py-1"></div>;
        }

        const cellId = `${row.original.event_id}-${fieldName}`;
        const isSelected = selectedCellId === cellId;

        if (isJSON(fieldName)) {
          return (
            <div className="px-2 py-1 flex justify-start">
              <div
                className={`flex items-center gap-2 cursor-pointer hover:bg-gray-200 rounded px-2 py-0.5 ${isSelected ? 'custom-selected-bg' : ''}`}
                onClick={() => onJsonCellToggle(cellId, value, fieldName)}
                data-cell-clickable="true"
              >
                <span className="text-sm">{getJsonSummary(value)}</span>
                <Search className={`h-3 w-3 ${isSelected ? 'custom-purple-icon' : 'text-muted-foreground'}`} />
              </div>
            </div>
          );
        }

        if (isTimestamp(fieldName)) {
          let formatted = String(value)
          try {
            const date = new Date(value as any);
            formatted = date.toLocaleString();
          } catch {
            // nothing to do
          }
          return (
            <div className="px-2 py-1">
              {formatted}
            </div>
          )
        }

        // Regular formatting for primitive values - use TruncatedCell for strings
        return <TruncatedCell value={String(value)} />;
      },
      enableSorting: !isJSON(fieldName),
      enableColumnFilter: !isTimestamp(fieldName) && !isJSON(fieldName),
      sortingFn: 'auto',
      filterFn: 'includesString',
    });
  });

  return columns;
}
