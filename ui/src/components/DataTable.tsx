import React, { useMemo, useState, useEffect } from 'react'
import {
  useReactTable,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  flexRender,
} from '@tanstack/react-table'
import type {
  SortingState,
  ColumnFiltersState,
  OnChangeFn,
} from '@tanstack/react-table'
import { DndProvider } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { TooltipProvider } from '@/components/ui/tooltip'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ChevronLeft, ChevronRight, Check, X } from 'lucide-react'

import { generateColumns } from '@/utils/column-generation'
import type { Event } from '@/services/api'
import { type ColumnMetadata } from '@/utils/column-metadata'
import { ColumnAutocomplete } from '@/components/ColumnAutocomplete'

type DataTableProps = {
  events: Event[]
  selectedColumns: ColumnMetadata[]
  selectedRowId: string | null
  selectedCellId: string | null
  columnFilters: ColumnFiltersState
  setColumnFilters: OnChangeFn<ColumnFiltersState>
  eventFilter: 'all' | 'valid' | 'failed'
  onEventFilterChange: (filter: 'all' | 'valid' | 'failed') => void
  onJsonCellToggle: (cellId: string, value: any, title: string) => void
  onReorderColumns: (fromIndex: number, toIndex: number) => void
  onRowClick: (rowId: string, event: Event) => void
}

export function DataTable({
  events,
  selectedColumns,
  selectedRowId,
  selectedCellId,
  columnFilters,
  setColumnFilters,
  eventFilter,
  onEventFilterChange,
  onJsonCellToggle,
  onReorderColumns,
  onRowClick,
}: DataTableProps) {
  const [sorting, setSorting] = useState<SortingState>([])
  const [initialSortSet, setInitialSortSet] = useState(false)

  // Set initial sorting when collector_tstamp is available
  useEffect(() => {
    if (!initialSortSet && selectedColumns.some(col => col.name === 'collector_tstamp')) {
      setSorting([{ id: 'collector_tstamp', desc: true }])
      setInitialSortSet(true)
    }
  }, [selectedColumns, initialSortSet])

  // Events are already in the correct format for react-table

  // Generate columns dynamically
  const columns = useMemo(() => {
    return generateColumns(
      selectedColumns,
      events,
      selectedCellId,
      eventFilter,
      onEventFilterChange,
      onJsonCellToggle,
      onReorderColumns
    )
  }, [selectedColumns, events, selectedCellId, eventFilter, onEventFilterChange, onJsonCellToggle, onReorderColumns])

  const table = useReactTable({
    data: events,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    state: {
      sorting,
      columnFilters,
    },
    initialState: {
      pagination: {
        pageSize: 50,
      },
    },
  })

  const totalRows = table.getFilteredRowModel().rows.length
  const currentPageRows = table.getRowModel().rows.length
  const pageIndex = table.getState().pagination.pageIndex
  const pageSize = table.getState().pagination.pageSize
  const startRow = pageIndex * pageSize + 1
  const endRow = Math.min((pageIndex + 1) * pageSize, totalRows)

  if (events.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground font-light">
        No events to display.
      </div>
    )
  }

  return (
    <TooltipProvider delayDuration={300}>
      <DndProvider backend={HTML5Backend}>
        <div className="h-full flex flex-col">
          {/* Table container with scrolling */}
          <div className="flex-1 overflow-auto rounded-md border">
            <Table className="table-auto">
              <TableHeader>
                {table.getHeaderGroups().map((headerGroup) => (
                  <React.Fragment key={headerGroup.id}>
                    <TableRow>
                      {headerGroup.headers.map((header) => (
                        <TableHead key={header.id}>
                          {header.isPlaceholder
                            ? null
                            : flexRender(
                                header.column.columnDef.header,
                                header.getContext()
                              )}
                        </TableHead>
                      ))}
                    </TableRow>
                    {/* Column filters row */}
                    <TableRow key={`${headerGroup.id}-filters`}>
                      {headerGroup.headers.map((header) => (
                        <TableHead key={`${header.id}-filter`} className="p-2">
                          {header.column.columnDef.meta?.eventStatusFilter ? (
                            <ToggleGroup
                              type="single"
                              value={eventFilter}
                              onValueChange={(value) =>
                                value &&
                                onEventFilterChange(value as 'all' | 'valid' | 'failed')
                              }
                              variant="outline"
                              size="sm"
                            >
                              <ToggleGroupItem value="all">All</ToggleGroupItem>
                              <ToggleGroupItem value="valid">
                                <Check className="h-3 w-3 text-success-dark" />
                              </ToggleGroupItem>
                              <ToggleGroupItem value="failed">
                                <X className="h-3 w-3 text-failure-dark" />
                              </ToggleGroupItem>
                            </ToggleGroup>
                          ) : header.column.getCanFilter() ? (
                            header.column.columnDef.meta?.useAutocomplete ? (
                              <ColumnAutocomplete
                                value={(header.column.getFilterValue() as string) ?? ''}
                                onChange={(value) => {
                                  header.column.setFilterValue(
                                    value === '' ? undefined : value
                                  )
                                }}
                                options={header.column.columnDef.meta?.distinctValues || []}
                                placeholder="Filter..."
                              />
                            ) : (
                              <Input
                                placeholder="Filter..."
                                value={
                                  (header.column.getFilterValue() as string) ?? ''
                                }
                                onChange={(e) => {
                                  const value = e.target.value
                                  header.column.setFilterValue(
                                    value === '' ? undefined : value
                                  )
                                }}
                                className="h-8 text-xs font-light"
                                style={{ width: '100px' }}
                              />
                            )
                          ) : null}
                        </TableHead>
                      ))}
                    </TableRow>
                  </React.Fragment>
                ))}
              </TableHeader>
              <TableBody>
                {currentPageRows > 0 ? (
                  table.getRowModel().rows.map((row) => {
                    const rowId = row.original.event_id || row.id || row.index
                    const isRowSelected = selectedRowId === rowId

                    return (
                      <TableRow
                        key={row.id}
                        data-state={row.getIsSelected() && 'selected'}
                        className={`cursor-pointer ${isRowSelected ? 'bg-gray-100 hover:bg-gray-100' : 'hover:bg-gray-50'}`}
                        onClick={(e) => {
                          // Don't trigger row click if clicking on a cell that has its own click handler
                          const target = e.target as HTMLElement
                          if (target.closest('[data-cell-clickable]')) {
                            return
                          }
                          onRowClick(String(rowId), row.original)
                        }}
                      >
                        {row.getVisibleCells().map((cell) => (
                          <TableCell key={cell.id}>
                            {flexRender(
                              cell.column.columnDef.cell,
                              cell.getContext()
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    )
                  })
                ) : (
                  <TableRow>
                    <TableCell
                      colSpan={columns.length}
                      className="h-24 text-center"
                    >
                      No events found.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>

          {/* Footer with row count and pagination - single line */}
          <div className="mt-4 flex items-center justify-between flex-shrink-0">
            {/* Row count on the left */}
            <div className="text-sm text-muted-foreground font-light">
              {totalRows === 0
                ? 'No events found'
                : totalRows === events.length
                  ? `Showing ${startRow}-${endRow} of ${totalRows} events`
                  : `Showing ${startRow}-${endRow} of ${totalRows} events (filtered from ${events.length} total)`}
            </div>

            {/* Pagination on the right */}
            {totalRows > pageSize && (
              <div className="flex items-center space-x-4">
                <div className="text-sm text-muted-foreground font-light">
                  Page {pageIndex + 1} of {table.getPageCount()}
                </div>
                <div className="flex items-center space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => table.previousPage()}
                    disabled={!table.getCanPreviousPage()}
                  >
                    <ChevronLeft className="h-4 w-4 mr-1" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => table.nextPage()}
                    disabled={!table.getCanNextPage()}
                  >
                    Next
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </DndProvider>
    </TooltipProvider>
  )
}
