import { useState, useEffect } from 'react';
import { DataTable } from '@/components/DataTable';
import { ColumnSelector } from '@/components/ColumnSelector';
import { JsonSidePanel } from '@/components/JsonSidePanel';
import { EventsChart } from '@/components/EventsChart';
import { type Event, EventsApiService } from '@/services/api';
import { useColumnManager } from '@/hooks/useColumnManager';
import { Button } from '@/components/ui/button';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { RefreshCw, Trash2, Menu } from 'lucide-react';


function App() {
  const [events, setEvents] = useState<Event[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showColumnSelector, setShowColumnSelector] = useState(false);
  const [selectedCellId, setSelectedCellId] = useState<string | null>(null);
  const [selectedRowId, setSelectedRowId] = useState<string | null>(null);
  const [jsonPanelData, setJsonPanelData] = useState<{ value: any; title: string } | null>(null);
  const [eventFilter, setEventFilter] = useState<'all' | 'valid' | 'failed'>('all');
  const [selectedMinute, setSelectedMinute] = useState<string | null>(null);

  const {
    availableColumns,
    selectedColumns,
    toggleColumn,
    reorderColumns: reorderSelectedColumns
  } = useColumnManager({ events });

  // Filter events based on status and time
  const filteredEvents = events.filter(event => {
    // Filter by status
    if (eventFilter !== 'all') {
      const failureData = event.contexts_com_snowplowanalytics_snowplow_failure_1;
      const hasFailures = failureData && Array.isArray(failureData) && failureData.length > 0;

      if (eventFilter === 'valid' && hasFailures) return false;
      if (eventFilter === 'failed' && !hasFailures) return false;
    }

    // Filter by selected minute
    if (selectedMinute && event.collector_tstamp) {
      const eventTime = new Date(event.collector_tstamp).getTime();
      const selectedTime = new Date(selectedMinute).getTime();
      const minuteStart = Math.floor(eventTime / 60000) * 60000;

      if (minuteStart !== selectedTime) return false;
    }

    return true;
  });

  const fetchEvents = async () => {
    setIsLoading(true);

    try {
      const fetchedEvents = await EventsApiService.fetchEvents();
      setEvents(fetchedEvents);
    } catch (err) {
      console.error('Failed to fetch events:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const resetEvents = async () => {
    setIsLoading(true);
    try {
      await EventsApiService.resetEvents();
      setEvents([]);
    } catch (err) {
      console.error('Failed to reset events:', err);
    } finally {
      setIsLoading(false);
    }
  };


  const toggleColumnSelector = () => {
    setShowColumnSelector(prev => !prev);
    // Close JSON panel when opening column selector
    if (!showColumnSelector) {
      setSelectedCellId(null);
      setSelectedRowId(null);
      setJsonPanelData(null);
    }
  };

  const openJsonPanel = (cellId: string, value: any, title: string) => {
    setSelectedCellId(cellId);
    setSelectedRowId(null); // Clear row selection when selecting cell
    setJsonPanelData({ value, title });
    // Close column selector when opening JSON panel
    setShowColumnSelector(false);

    // Scroll selected cell into view after a short delay
    setTimeout(() => {
      const selectedElement = document.querySelector(`[data-cell-clickable="true"].custom-selected-bg`);
      if (selectedElement) {
        selectedElement.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest',
          inline: 'nearest'
        });
      }
    }, 100);
  };

  const closeJsonPanel = () => {
    setSelectedCellId(null);
    setSelectedRowId(null);
    setJsonPanelData(null);
  };

  const toggleJsonPanel = (cellId: string, value: any, title: string) => {
    if (selectedCellId === cellId) {
      closeJsonPanel();
    } else {
      openJsonPanel(cellId, value, title);
    }
  };

  const handleRowClick = (rowId: string, event: Event) => {
    if (selectedRowId === rowId) {
      closeJsonPanel();
    } else {
      setSelectedRowId(rowId);
      setSelectedCellId(null); // Clear cell selection when selecting row
      setJsonPanelData({ value: event, title: 'Full event' });
      setShowColumnSelector(false);
    }
  };

  const handleMinuteClick = (minute: string | null) => {
    setSelectedMinute(minute);
  };

  // Initial load
  useEffect(() => {
    fetchEvents();
  }, []);

  return (
    <div className="min-h-screen bg-background">
      <div className="flex h-screen min-w-0">
        <div className="flex-1 flex flex-col min-w-0">
          {/* Header */}
          <div className="border-b bg-background p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <img
                  src={`${import.meta.env.BASE_URL}/logo.png`}
                  alt="Snowplow Micro"
                  className="h-8"
                />
              </div>
              <div className="flex items-center space-x-2">
                <div className="px-4">
                  <ToggleGroup
                    type="single"
                    value={eventFilter}
                    onValueChange={(value) => value && setEventFilter(value as 'all' | 'valid' | 'failed')}
                    variant="outline"
                    size="sm"
                  >
                    <ToggleGroupItem value="all">All events</ToggleGroupItem>
                    <ToggleGroupItem value="valid">
                      <div className="flex items-center gap-1.5">
                        <div className="w-2 h-2 rounded-full bg-(--dark-teal)"></div>
                        Valid
                      </div>
                    </ToggleGroupItem>
                    <ToggleGroupItem value="failed">
                      <div className="flex items-center gap-1.5">
                        <div className="w-2 h-2 rounded-full bg-(--dark-orange)"></div>
                        Failed
                      </div>
                    </ToggleGroupItem>
                  </ToggleGroup>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchEvents()}
                  disabled={isLoading}
                  className="cursor-pointer"
                >
                  <RefreshCw className={`mr-2 h-4 w-4 stroke-(--custom-purple) ${isLoading ? 'animate-spin' : ''}`} />
                  Refresh
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={resetEvents}
                  className="cursor-pointer"
                >
                  <Trash2 className="mr-2 h-4 w-4 stroke-(--dark-orange)" />
                  Delete all events
                </Button>
                <Button
                  variant={showColumnSelector ? "default" : "outline"}
                  size="sm"
                  onClick={toggleColumnSelector}
                  className="cursor-pointer"
                >
                  <Menu className="mr-2 h-4 w-4 rotate-90" />
                  Pick columns
                </Button>
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
                    onMinuteClick={handleMinuteClick}
                  />
                </div>

                {/* Data Table */}
                <div className="flex-1 min-h-0">
                  <DataTable
                    events={filteredEvents}
                    selectedColumns={selectedColumns}
                    onJsonCellToggle={toggleJsonPanel}
                    selectedCellId={selectedCellId}
                    onReorderColumns={reorderSelectedColumns}
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
            maxDepth={jsonPanelData.title === 'Full event' ? 1 : jsonPanelData.title === 'Failures' ? 4 : undefined}
          />
        )}
      </div>
    </div>
  );
}

export default App;