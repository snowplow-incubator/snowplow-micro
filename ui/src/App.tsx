import { useState, useEffect } from 'react'
import { DataTable } from '@/components/DataTable'
import { ColumnSelector } from '@/components/ColumnSelector'
import { JsonSidePanel } from '@/components/JsonSidePanel'
import { EventsChart } from '@/components/EventsChart'
import { type Event, EventsApiService } from '@/services/api'
import { useColumnManager } from '@/hooks/useColumnManager'
import { Button } from '@/components/ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { RefreshCw, Trash2, Menu, FilterX } from 'lucide-react'
import { type ColumnFiltersState } from '@tanstack/react-table'
import { hasFailureData, roundToMinute } from '@/utils/event-utils'

function App() {
  const [events, setEvents] = useState<Event[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [showColumnSelector, setShowColumnSelector] = useState(false)
  const [selectedCellId, setSelectedCellId] = useState<string | null>(null)
  const [selectedRowId, setSelectedRowId] = useState<string | null>(null)
  const [jsonPanelData, setJsonPanelData] = useState<{
    value: any
    title: string
  } | null>(null)
  const [eventFilter, setEventFilter] = useState<'all' | 'valid' | 'failed'>(
    'all'
  )
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [selectedMinute, setSelectedMinute] = useState<string | null>(null)

  // Handle scrolling to newly added columns
  const scrollToLastColumn = () => {
    setTimeout(() => {
      const headers = document.querySelectorAll('thead th')
      const lastHeader = headers[headers.length - 1]
      if (lastHeader) {
        lastHeader.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest',
          inline: 'nearest',
        })
      }
    }, 100)
  }

  const { availableColumns, selectedColumns, toggleColumn, reorderColumns } =
    useColumnManager({ events, setColumnFilters, onColumnAdded: scrollToLastColumn })

  // Filter events based on status and time
  const filteredEvents = events.filter((event) => {
    // Filter by status
    if (eventFilter !== 'all') {
      const hasFailures = hasFailureData(event)
      if (eventFilter === 'valid' && hasFailures) return false
      if (eventFilter === 'failed' && !hasFailures) return false
    }

    // Filter by selected minute
    if (selectedMinute && event.collector_tstamp) {
      const eventTime = new Date(event.collector_tstamp).getTime()
      const selectedTime = new Date(selectedMinute).getTime()
      const minuteStart = roundToMinute(eventTime)

      if (minuteStart !== selectedTime) return false
    }

    return true
  })

  const fetchEvents = async () => {
    setIsLoading(true)

    try {
      const fetchedEvents = await EventsApiService.fetchEvents()
      setEvents(fetchedEvents)
    } catch (err) {
      console.error('Failed to fetch events:', err)
    } finally {
      setIsLoading(false)
    }
  }

  const resetEvents = async () => {
    const confirmed = confirm(
      'Are you sure you want to delete all events? This action cannot be undone.'
    )
    if (!confirmed) return

    setIsLoading(true)
    try {
      await EventsApiService.resetEvents()
      setEvents([])
    } catch (err) {
      console.error('Failed to reset events:', err)
    } finally {
      setIsLoading(false)
    }
  }

  const closeJsonPanel = () => {
    setSelectedCellId(null)
    setSelectedRowId(null)
    setJsonPanelData(null)
  }

  const toggleColumnSelector = () => {
    setShowColumnSelector((prev) => !prev)
    if (!showColumnSelector) {
      closeJsonPanel()
    }
  }

  const openJsonPanel = (cellId: string, value: any, title: string) => {
    setSelectedCellId(cellId)
    setSelectedRowId(null)
    setJsonPanelData({ value, title })
    setShowColumnSelector(false)

    setTimeout(() => {
      const selectedElement = document.querySelector(
        `[data-cell-clickable="true"].bg-gray-200`
      )
      if (selectedElement) {
        selectedElement.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest',
          inline: 'nearest',
        })
      }
    }, 100)
  }

  const toggleJsonPanel = (cellId: string, value: any, title: string) => {
    if (selectedCellId === cellId) {
      closeJsonPanel()
    } else {
      openJsonPanel(cellId, value, title)
    }
  }

  const handleRowClick = (rowId: string, event: Event) => {
    if (selectedRowId === rowId) {
      closeJsonPanel()
    } else {
      setSelectedRowId(rowId)
      setSelectedCellId(null)
      setJsonPanelData({ value: event, title: 'Full event' })
      setShowColumnSelector(false)
    }
  }


  // Check if any filters are active
  const hasActiveFilters =
    eventFilter !== 'all' || selectedMinute !== null || columnFilters.length > 0

  // Reset all filters
  const resetAllFilters = () => {
    setEventFilter('all')
    setSelectedMinute(null)
    setColumnFilters([])
  }

  // Get active filters for tooltip
  const getActiveFilters = () => {
    const filters: string[] = []

    if (eventFilter !== 'all') {
      filters.push(`Event type: ${eventFilter}`)
    }

    if (selectedMinute) {
      const date = new Date(selectedMinute)
      filters.push(`Time: ${date.toLocaleTimeString()}`)
    }

    if (columnFilters.length > 0) {
      columnFilters.forEach((filter) => {
        filters.push(`${filter.id}: ${filter.value}`)
      })
    }

    return filters
  }

  // Handle ESC key to close side panels
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (showColumnSelector) {
          setShowColumnSelector(false)
        } else if (jsonPanelData) {
          closeJsonPanel()
        }
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [showColumnSelector, jsonPanelData])

  // Initial load
  useEffect(() => {
    fetchEvents()
  }, [])

  return (
    <div className="min-h-screen bg-background">
      <div className="flex h-screen min-w-0">
        <div className="flex-1 flex flex-col min-w-0">
          {/* Header */}
          <div className="border-b bg-background p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <img
                  src="/micro/ui/logo.png"
                  alt="Snowplow Micro"
                  className="h-8"
                />
              </div>
              <div className="flex items-center gap-4">
                {hasActiveFilters && (
                  <TooltipProvider>
                    <Tooltip delayDuration={0}>
                      <TooltipTrigger asChild>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={resetAllFilters}
                        >
                          <FilterX className="mr-2 h-4 w-4" />
                          Reset filters
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>
                        <div>
                          <div className="font-medium">Active filters:</div>
                          {getActiveFilters().map((filter, index) => (
                            <div key={index} className="text-xs">
                              • {filter}
                            </div>
                          ))}
                        </div>
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                )}

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => fetchEvents()}
                    disabled={isLoading}
                  >
                    <RefreshCw
                      className={`mr-2 h-4 w-4 stroke-brand-primary ${isLoading ? 'animate-spin' : ''}`}
                    />
                    Refresh
                  </Button>
                  <Button variant="outline" size="sm" onClick={resetEvents}>
                    <Trash2 className="mr-2 h-4 w-4 stroke-failure" />
                    Delete all events
                  </Button>
                  <Button
                    variant={showColumnSelector ? 'default' : 'outline'}
                    size="sm"
                    onClick={toggleColumnSelector}
                  >
                    <Menu className="mr-2 h-4 w-4 rotate-90" />
                    Pick columns
                  </Button>
                </div>
              </div>
            </div>
          </div>

          {/* Main Content */}
          <div className="flex-1 overflow-hidden min-w-0">
            {/* Loading State */}
            {isLoading && events.length === 0 ? (
              <div className="flex items-center justify-center h-full">
                <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                <span className="font-light">Loading events...</span>
              </div>
            ) : (
              <div className="h-full p-4 min-w-0 flex flex-col">
                {/* Events Chart */}
                <div className="mb-4">
                  <EventsChart
                    events={events}
                    selectedMinute={selectedMinute}
                    onMinuteClick={setSelectedMinute}
                  />
                </div>

                {/* Data Table */}
                <div className="flex-1 min-h-0">
                  <DataTable
                    events={filteredEvents}
                    selectedColumns={selectedColumns}
                    selectedCellId={selectedCellId}
                    columnFilters={columnFilters}
                    setColumnFilters={setColumnFilters}
                    eventFilter={eventFilter}
                    onEventFilterChange={setEventFilter}
                    onJsonCellToggle={toggleJsonPanel}
                    onReorderColumns={reorderColumns}
                    onRowClick={handleRowClick}
                    selectedRowId={selectedRowId}
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Column Selector Sidebar */}
        {showColumnSelector && (
          <div className="w-80 min-w-[200px] flex-shrink-0">
            <ColumnSelector
              availableColumns={availableColumns}
              selectedColumns={selectedColumns}
              onToggleColumn={toggleColumn}
              onClose={() => setShowColumnSelector(false)}
            />
          </div>
        )}

        {/* JSON Side Panel */}
        {jsonPanelData && (
          <JsonSidePanel
            value={jsonPanelData.value}
            title={jsonPanelData.title}
            onClose={closeJsonPanel}
            maxDepth={
              jsonPanelData.title === 'Full event'
                ? 1
                : jsonPanelData.title === 'Failures'
                  ? 4
                  : undefined
            }
          />
        )}
      </div>
    </div>
  )
}

export default App
