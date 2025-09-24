import { useState, useEffect, useRef } from 'react'
import { Search, X } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { TooltipProvider } from '@/components/ui/tooltip'
import { TruncatedColumnName } from '@/components/TruncatedColumnName'
import { type ColumnMetadata } from '@/utils/column-metadata'

type ColumnSelectorProps = {
  availableColumns: ColumnMetadata[]
  selectedColumns: ColumnMetadata[]
  onToggleColumn: (fieldName: string) => void
  onClose: () => void
}

export function ColumnSelector({
  availableColumns,
  selectedColumns,
  onToggleColumn,
  onClose,
}: ColumnSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedTab, setSelectedTab] = useState<
    'selected' | 'atomic' | 'events' | 'entities'
  >('selected')
  const searchInputRef = useRef<HTMLInputElement>(null)

  // Auto-focus search input when component mounts or tab changes
  useEffect(() => {
    if (searchInputRef.current) {
      searchInputRef.current.focus()
    }
  }, [selectedTab])

  const selected = new Set(selectedColumns.map(col => col.name))

  const filtered = availableColumns
    .filter((columnMetadata) => {
      const { name } = columnMetadata
      // Handle "Selected" tab - show only selected columns
      if (selectedTab === 'selected') {
        const matchesSelection = selected.has(name)
        if (!matchesSelection) return false

        // Filter by search term - search the full column name for selected tab
        if (searchTerm) {
          return name.toLowerCase().includes(searchTerm.toLowerCase())
        }
        return true
      }

      // Get the parent column name for nested fields
      const parentColumn = columnMetadata.isNested ? columnMetadata.parentColumn! : name

      // First filter by tab based on parent column
      let matchesTab = false
      if (selectedTab === 'events') {
        matchesTab = parentColumn.startsWith('unstruct_event_')
      } else if (selectedTab === 'entities') {
        matchesTab = parentColumn.startsWith('contexts_')
      } else {
        // atomic
        matchesTab =
          !parentColumn.startsWith('unstruct_event_') &&
          !parentColumn.startsWith('contexts_')
      }

      if (!matchesTab) return false

      // Then filter by search term - only search the parent column name
      if (searchTerm) {
        return parentColumn.toLowerCase().includes(searchTerm.toLowerCase())
      }

      return true
    })
    .sort((a, b) => a.name.localeCompare(b.name))

  return (
    <TooltipProvider>
      <div className="border-l bg-background h-full overflow-hidden flex flex-col">
        {/* Header with title and close button */}
        <div className="border-b bg-background p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-light truncate">Columns</h2>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="h-8 w-8 p-0"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </div>

        <div className="p-4 flex-shrink-0">
          <div className="space-y-3">
            {/* Toggle button group */}
            <ToggleGroup
              type="single"
              value={selectedTab}
              onValueChange={(value) =>
                value &&
                setSelectedTab(
                  value as 'selected' | 'atomic' | 'events' | 'entities'
                )
              }
              variant="outline"
              size="sm"
              className="w-full"
            >
              <ToggleGroupItem value="selected" className="flex-1">
                Selected
              </ToggleGroupItem>
              <ToggleGroupItem value="atomic" className="flex-1">
                Atomic
              </ToggleGroupItem>
              <ToggleGroupItem value="events" className="flex-1">
                Events
              </ToggleGroupItem>
              <ToggleGroupItem value="entities" className="flex-1">
                Entities
              </ToggleGroupItem>
            </ToggleGroup>

            {/* Search input */}
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                ref={searchInputRef}
                placeholder="Search columns..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 font-light"
              />
            </div>
          </div>
        </div>

        {/* Column list */}
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {filtered.length === 0 ? (
            <div className="text-center text-muted-foreground py-8 text-sm font-light">
              {searchTerm
                ? `No columns found matching "${searchTerm}"`
                : 'No columns found'}
            </div>
          ) : (
            filtered.map((columnMetadata) => {
              const { name, isNested, fieldName } = columnMetadata
              // for Selected tab, always show full name and icon
              // for other tabs, show field name only for nested columns and no icon
              const updatedColumnMetadata = selectedTab !== 'selected' && isNested ?
                {...columnMetadata, icon: null, truncatedName: fieldName} : columnMetadata

              // for Selected tab, don't indent nested fields
              const shouldIndent = selectedTab !== 'selected' && isNested

              return (
                <div
                  key={name}
                  className={`flex items-center gap-3 p-1 rounded hover:bg-muted/50 cursor-pointer transition-colors ${
                    shouldIndent ? 'ml-6' : ''
                  }`}
                  onClick={() => onToggleColumn(name)}
                >
                  {/* Checkbox on the left */}
                  <input
                    type="checkbox"
                    checked={selected.has(name)}
                    onChange={() => onToggleColumn(name)}
                    className="h-4 w-4 rounded border-muted-foreground cursor-pointer"
                    onClick={(e) => e.stopPropagation()}
                  />

                  {/* Field name with truncation and tooltip */}
                  <div className="flex-1 min-w-0">
                    <TruncatedColumnName
                      columnMetadata={updatedColumnMetadata}
                      className="text-sm font-light"
                    />
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>
    </TooltipProvider>
  )
}
