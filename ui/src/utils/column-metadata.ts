import { truncateMiddle } from './event-utils'

export type ColumnMetadata = {
  name: string                    // original name (e.g., "contexts_schema.field")
  isNested: boolean
  isJSON: boolean
  isTimestamp: boolean
  parentColumn?: string
  fieldName?: string
  truncatedName?: string          // only present if truncated
  icon: 'entity' | 'event' | null
}

/**
 * Create metadata for a single column
 */
export function createColumnMetadata(columnName: string): ColumnMetadata {
  const isNested = columnName.includes('.')
  const isJSON = columnName.startsWith('unstruct_event_') || columnName.startsWith('contexts_')
  const isTimestamp = columnName.endsWith('_tstamp')

  let displayText = columnName
  let icon: 'entity' | 'event' | null = null
  let wasTruncated = false

  // Check for contexts_ prefix (removing prefix counts as truncation)
  if (displayText.startsWith('contexts_')) {
    displayText = displayText.replace('contexts_', '')
    icon = 'entity'
    wasTruncated = true
  }

  // Check for unstruct_event_ prefix (removing prefix counts as truncation)
  if (displayText.startsWith('unstruct_event_')) {
    displayText = displayText.replace('unstruct_event_', '')
    icon = 'event'
    wasTruncated = true
  }

  // Additional truncation if still too long
  if (displayText.length > 35) {
    displayText = truncateMiddle(displayText)
    wasTruncated = true
  }

  return {
    name: columnName,
    isNested,
    isJSON,
    isTimestamp,
    parentColumn: isNested ? columnName.substring(0, columnName.indexOf('.')) : undefined,
    fieldName: isNested ? columnName.substring(columnName.indexOf('.') + 1) : undefined,
    truncatedName: wasTruncated ? displayText : undefined,
    icon
  }
}