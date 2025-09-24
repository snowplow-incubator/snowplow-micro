import { useState } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CopyButton } from '@/components/CopyButton'

type JsonViewerProps = {
  data: any
  currentDepth?: number
  maxDepth?: number
  keyName?: string | number
  autoExpand?: boolean
}

export function JsonViewer({
  data,
  currentDepth = 0,
  maxDepth,
  keyName,
  autoExpand = false,
}: JsonViewerProps) {
  const [isExpanded, setIsExpanded] = useState(
    maxDepth === undefined || currentDepth < maxDepth || autoExpand
  )

  const handleToggleExpanded = () => setIsExpanded(!isExpanded)

  // Helper to render key part
  const renderKey = (isClickable = false) => {
    if (keyName === undefined || keyName === null) return null
    const keyText = typeof keyName === 'string' ? `"${keyName}"` : keyName

    if (isClickable) {
      return (
        <span
          className="text-gray-600 text-xs font-mono cursor-pointer"
          onClick={handleToggleExpanded}
        >
          {keyText}:
        </span>
      )
    }

    return <span className="text-gray-600 text-xs font-mono">{keyText}:</span>
  }

  if (data === null) {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="text-muted-foreground text-xs font-mono">null</span>
        <CopyButton value={null} />
      </div>
    )
  }

  if (data === undefined) {
    return (
      <div className="inline-flex items-center gap-1">
        {renderKey()}
        <span className="text-muted-foreground text-xs font-mono">
          undefined
        </span>
      </div>
    )
  }

  if (typeof data === 'string') {
    return (
      <div className="inline-flex items-start gap-1 group">
        {renderKey()}
        <span className="text-info text-xs font-mono break-all">
          "{data}"
        </span>
        <CopyButton value={data} />
      </div>
    )
  }

  if (typeof data === 'number') {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="text-info text-xs font-mono">{data}</span>
        <CopyButton value={data} />
      </div>
    )
  }

  if (typeof data === 'boolean') {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="text-info text-xs font-mono">
          {data.toString()}
        </span>
        <CopyButton value={data} />
      </div>
    )
  }

  if (Array.isArray(data)) {
    if (data.length === 0) {
      return (
        <div className="inline-flex items-center gap-1 group">
          {renderKey()}
          <span className="text-muted-foreground text-xs font-mono">[]</span>
          <CopyButton value={data} />
        </div>
      )
    }

    return (
      <div className="flex flex-col">
        <div className="inline-flex items-center gap-1 group">
          {renderKey(true)}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleToggleExpanded}
            className="h-6 px-1 text-xs"
          >
            {isExpanded ? (
              <ChevronDown className="w-3 h-3 mr-1" />
            ) : (
              <ChevronRight className="w-3 h-3 mr-1" />
            )}
            <span className="text-muted-foreground font-mono">
              [{data.length}]
            </span>
          </Button>
          <CopyButton value={data} />
        </div>
        {isExpanded && (
          <div className="ml-3 mb-1 border-l border-muted pl-2">
            {data.map((item, index) => (
              <div key={index}>
                <JsonViewer
                  data={item}
                  currentDepth={currentDepth + 1}
                  maxDepth={maxDepth}
                  autoExpand={data.length === 1}
                  keyName={index}
                />
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }

  if (typeof data === 'object') {
    const keys = Object.keys(data)
    if (keys.length === 0) {
      return (
        <div className="inline-flex items-center gap-1 group">
          {renderKey()}
          <span className="text-muted-foreground text-xs font-mono">
            {'{}'}
          </span>
          <CopyButton value={data} />
        </div>
      )
    }

    return (
      <div className="flex flex-col">
        <div className="inline-flex items-center gap-1 group">
          {renderKey(true)}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleToggleExpanded}
            className="h-6 px-1 text-xs"
          >
            {isExpanded ? (
              <ChevronDown className="w-3 h-3 mr-1" />
            ) : (
              <ChevronRight className="w-3 h-3 mr-1" />
            )}
            <span className="text-muted-foreground font-mono">
              {`{${keys.length}}`}
            </span>
          </Button>
          <CopyButton value={data} />
        </div>
        {isExpanded && (
          <div className="ml-3 mb-1 border-l border-muted pl-2">
            {sortKeys(keys).map((key) => (
              <div key={key}>
                <JsonViewer
                  data={data[key]}
                  currentDepth={currentDepth + 1}
                  maxDepth={maxDepth}
                  autoExpand={keys.length === 1}
                  keyName={key}
                />
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <span className="text-muted-foreground text-xs font-mono">
      {String(data)}
    </span>
  )
}

/**
 * Sort keys with priority: unstruct_event_ first, contexts_ second, then alphabetically
 */
function sortKeys(keys: string[]): string[] {
  return keys.sort((a, b) => {
    const aIsUnstruct = a.startsWith('unstruct_event_')
    const bIsUnstruct = b.startsWith('unstruct_event_')
    const aIsContext = a.startsWith('contexts_')
    const bIsContext = b.startsWith('contexts_')

    // Both unstruct_event_: sort alphabetically within category
    if (aIsUnstruct && bIsUnstruct) {
      return a.localeCompare(b)
    }

    // Both contexts_: sort alphabetically within category
    if (aIsContext && bIsContext) {
      return a.localeCompare(b)
    }

    // unstruct_event_ comes first
    if (aIsUnstruct && !bIsUnstruct) return -1
    if (!aIsUnstruct && bIsUnstruct) return 1

    // contexts_ comes second (after unstruct_event_)
    if (aIsContext && !bIsContext && !bIsUnstruct) return -1
    if (!aIsContext && bIsContext && !aIsUnstruct) return 1

    // Everything else: alphabetically
    return a.localeCompare(b)
  })
}
