import { useState, useMemo, useEffect } from 'react';

import { type Event } from '@/services/api';

const STORAGE_KEY = 'snowplow-micro-selected-columns';

// Default column configuration
const DEFAULT_COLUMNS = [
  'collector_tstamp',
  'app_id',
  'event_name'
] as const;

/**
 * Save selected columns to localStorage
 */
function saveSelectedColumns(columns: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(columns));
  } catch (error) {
    console.warn('Failed to save selected columns to localStorage:', error);
  }
}

/**
 * Load selected columns from localStorage
 */
function loadSelectedColumns(): string[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      if (Array.isArray(parsed) && parsed.every(item => typeof item === 'string')) {
        return parsed;
      }
    }
  } catch (error) {
    console.warn('Failed to load selected columns from localStorage:', error);
  }

  return [...DEFAULT_COLUMNS];
}

type UseColumnManagerProps = {
  events: Event[];
}

type UseColumnManagerReturn = {
  availableColumns: string[],
  selectedColumns: string[],
  toggleColumn: (fieldName: string) => void,
  reorderColumns: (fromIndex: number, toIndex: number) => void
}

export function useColumnManager({ events }: UseColumnManagerProps): UseColumnManagerReturn {
  const [selectedColumns, setSelectedColumns] = useState<string[]>(loadSelectedColumns())

  useEffect(() => {
    saveSelectedColumns(selectedColumns)
  }, [selectedColumns])

  const availableColumns = useMemo(() => {
    const set = new Set([
      ...events.flatMap(event => Object.keys(event)),
      ...selectedColumns
    ])
    // pinned column not available for selection
    set.delete("contexts_com_snowplowanalytics_snowplow_failure_1")
    return Array.from(set)
  }, [events, selectedColumns]);

  const toggleColumn = (fieldName: string) => {
    if (selectedColumns.includes(fieldName)) {
      const updated = selectedColumns.filter(c => c != fieldName)
      setSelectedColumns(updated)
    } else {
      const updated = [...selectedColumns, fieldName]
      setSelectedColumns(updated);
    }
  }

  const reorderColumns = (fromIndex: number, toIndex: number) => {
    const reordered = [...selectedColumns]
    const column = reordered.splice(fromIndex, 1)[0]
    reordered.splice(toIndex, 0, column)
    setSelectedColumns(reordered)
  }

  return {
    availableColumns,
    selectedColumns,
    toggleColumn,
    reorderColumns
  };
}