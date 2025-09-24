import { useState } from 'react';
import { Search, X } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { TooltipProvider } from '@/components/ui/tooltip';
import { sortKeys } from '@/utils/sort-keys';
import { TruncatedColumnName } from '@/components/TruncatedColumnName';

type ColumnSelectorProps = {
  availableColumns: string[],
  selectedColumns: string[],
  onToggleColumn: (fieldName: string) => void;
  onClose: () => void;
}

export function ColumnSelector({
  availableColumns,
  selectedColumns,
  onToggleColumn,
  onClose
}: ColumnSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('');

  const filtered = sortKeys(availableColumns.filter(name =>
    name.toLowerCase().includes(searchTerm.toLowerCase())
  ));

  const selected = new Set(selectedColumns)

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
            className="h-8 w-8 p-0 cursor-pointer"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="p-4 pb-0 flex-1 overflow-hidden flex flex-col">
        <div className="mb-4">

        {/* Search input */}
        <div className="relative">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search fields..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-8 font-light"
          />
        </div>
      </div>

      {/* Column list */}
      <div className="flex-1 overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="text-center text-muted-foreground py-8 text-sm font-light">
            No fields found matching "{searchTerm}"
          </div>
        ) : (
          filtered.map(name => (
            <div
              key={name}
              className="flex items-center gap-3 p-1 rounded hover:bg-muted/50 cursor-pointer transition-colors"
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
                <TruncatedColumnName columnName={name} className="text-sm font-light" />
              </div>
            </div>
          ))
        )}
      </div>
      </div>
    </div>
    </TooltipProvider>
  );
}
