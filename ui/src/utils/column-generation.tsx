import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { ArrowUp, ArrowDown, Eye, Check, X } from 'lucide-react'
import type { Event } from '@/services/api'
import { DraggableColumn } from '@/components/DraggableColumn'
import { TruncatedCell } from '@/components/TruncatedCell'
import { TruncatedColumnName } from '@/components/TruncatedColumnName'
import {
  truncateJsonForDisplay,
  valueToSearchableString,
} from './json-fields'
import { type ColumnMetadata } from './column-metadata'


/**
 * Get distinct values for a column from the data
 */
function getDistinctValues(events: Event[], columnMetadata: ColumnMetadata): string[] {
  const values = new Set<string>()

  events.forEach((event) => {
    const value = columnMetadata.accessor(event)
    if (value !== undefined && value !== null) {
      if (columnMetadata.isJSON) {
        // For JSON columns, use the searchable string representation
        values.add(valueToSearchableString(value))
      } else {
        values.add(String(value))
      }
    }
  })

  return Array.from(values).sort()
}

/**
 * Generate columns from selected fields and available field info
 */
export function generateColumns(
  selectedColumns: ColumnMetadata[],
  events: Event[],
  selectedCellId: string | null,
  eventFilter: 'all' | 'valid' | 'failed',
  onEventFilterChange: (filter: 'all' | 'valid' | 'failed') => void,
  onJsonCellToggle: (cellId: string, value: any, title: string) => void,
  onReorderColumns: (fromIndex: number, toIndex: number) => void
): ColumnDef<Event>[] {
  const columns: ColumnDef<Event>[] = []

  // Pinned status column
  columns.push({
    accessorKey: 'contexts_com_snowplowanalytics_snowplow_failure_1',
    header: () => (
      <div className="text-center">Status</div>
    ),
    meta: {
      eventStatusFilter: true,
    },
    cell: ({ getValue, row }) => {
      const value = getValue()
      const cellId = `${row.original.event_id}-status`
      const isSelected = selectedCellId === cellId

      // Handle undefined values - show OK status
      if (value === undefined || (Array.isArray(value) && value.length === 0)) {
        return (
          <div className="px-2 py-1 flex justify-center">
            <span className="px-3 py-1 rounded-full text-xs whitespace-nowrap bg-success-light text-success-dark font-normal flex items-center gap-1">
              <Check className="h-3 w-3" />
              Valid
            </span>
          </div>
        )
      }

      if (Array.isArray(value) && onJsonCellToggle) {
        const failureCount = value.length
        return (
          <div
            className="px-2 py-1 flex items-center justify-center gap-2 cursor-pointer group"
            onClick={() =>
              onJsonCellToggle(
                cellId,
                value.map((v) => ({
                  data: v.data,
                  errors: v.errors,
                  failureType: v.failureType,
                })),
                'Failures'
              )
            }
            data-cell-clickable="true"
          >
            <span
              className={`px-2 py-1 rounded-full text-xs font-normal whitespace-nowrap transition-colors flex items-center gap-1 ${
                isSelected
                  ? 'bg-failure text-failure-dark'
                  : 'bg-failure-light text-failure-dark group-hover:bg-failure'
              }`}
            >
              <X className="h-3 w-3" />
              {failureCount} {failureCount === 1 ? 'failure' : 'failures'}
            </span>
          </div>
        )
      }

      // Regular formatting for primitive values
      return <div className="px-2 py-1">{String(value)}</div>
    },
    enableSorting: false,
    enableColumnFilter: false,
  })

  // Add selected columns
  selectedColumns.forEach((columnMetadata, index) => {
    const { name: fieldName } = columnMetadata

    // Get distinct values for filterable columns (exclude only timestamps)
    const distinctValues = !columnMetadata.isTimestamp
      ? getDistinctValues(events, columnMetadata)
      : []
    const useAutocomplete = distinctValues.length > 0 && distinctValues.length <= 10

    const columnDef: ColumnDef<Event> = {
      id: fieldName,
      accessorFn: columnMetadata.accessor,
      meta: {
        useAutocomplete,
        distinctValues,
      },
      header: ({ column }) => {
        return (
          <DraggableColumn index={index} onReorder={onReorderColumns}>
            <Button
              variant="ghost"
              onClick={() =>
                column.toggleSorting(column.getIsSorted() !== 'desc')
              }
              className="h-8 px-2 flex items-center group"
              title="Sort column"
            >
              <TruncatedColumnName columnMetadata={columnMetadata} />
              <div className="ml-2">
                {column.getIsSorted() === 'asc' ? (
                  <ArrowUp className="h-4 w-4" />
                ) : column.getIsSorted() === 'desc' ? (
                  <ArrowDown className="h-4 w-4" />
                ) : (
                  <ArrowDown className="h-4 w-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                )}
              </div>
            </Button>
          </DraggableColumn>
        )
      },
      cell: ({ getValue, row }) => {
        const value = getValue()

        // Handle undefined values - show empty cell
        if (value === undefined) {
          return <div className="px-2 py-1"></div>
        }

        const cellId = `${row.original.event_id}-${fieldName}`
        const isSelected = selectedCellId === cellId

        if (columnMetadata.isJSON) {
          // For nested fields, show the parent column data in the JSON viewer
          const jsonViewerData = columnMetadata.isNested
            ? row.original[columnMetadata.parentColumn!]
            : value
          const jsonViewerTitle = columnMetadata.isNested
            ? columnMetadata.parentColumn!
            : fieldName

          return (
            <div className="px-2 py-1 flex justify-start">
              <div
                className={`flex items-center gap-2 cursor-pointer hover:bg-gray-200 rounded px-2 py-0.5 ${isSelected ? 'bg-gray-200' : ''}`}
                onClick={() =>
                  onJsonCellToggle(cellId, jsonViewerData, jsonViewerTitle)
                }
                data-cell-clickable="true"
              >
                <Eye className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm whitespace-pre-wrap">
                  {truncateJsonForDisplay(value, fieldName, 100)}
                </span>
              </div>
            </div>
          )
        }

        if (columnMetadata.isTimestamp) {
          let formatted = String(value)
          try {
            const date = new Date(value as any)
            formatted = date.toLocaleString()
          } catch {
            // nothing to do
          }
          return <div className="px-2 py-1">{formatted}</div>
        }

        // Regular formatting for primitive values - use TruncatedCell for strings
        return <TruncatedCell value={String(value)} />
      },
      enableSorting: true,
      enableColumnFilter: !columnMetadata.isTimestamp,
      sortingFn: columnMetadata.isJSON
        ? (rowA, rowB, columnId) => {
            const aValue = rowA.getValue(columnId)
            const bValue = rowB.getValue(columnId)
            const aString = valueToSearchableString(aValue)
            const bString = valueToSearchableString(bValue)
            return aString.localeCompare(bString)
          }
        : 'auto',
      filterFn: columnMetadata.isJSON
        ? (row, columnId, filterValue) => {
            const value = row.getValue(columnId)
            const searchableString = valueToSearchableString(value)
            return searchableString
              .toLowerCase()
              .includes(filterValue.toLowerCase())
          }
        : 'includesString',
    }

    columns.push(columnDef)
  })

  return columns
}
