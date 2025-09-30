import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { JsonViewer } from '@/components/JsonViewer'
import { TooltipProvider } from '@/components/ui/tooltip'
import { TruncatedColumnName } from '@/components/TruncatedColumnName'
import { createColumnMetadata } from '@/utils/column-metadata'

type JsonSidePanelProps = {
  value: any
  title: string
  onClose: () => void
  maxDepth?: number
}

export function JsonSidePanel({
  value,
  title,
  onClose,
  maxDepth,
}: JsonSidePanelProps) {
  return (
    <TooltipProvider>
      <div className="w-96 min-w-[400px] flex-shrink-0 border-l bg-background h-full overflow-hidden flex flex-col p-4 gap-4">
        {/* Header with title and close button */}
        <div className="bg-background">
          <div className="flex items-center justify-between">
            <TruncatedColumnName
              columnMetadata={createColumnMetadata(title)}
              iconSize="md"
              className="text-lg font-light truncate"
            />
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

        {/* JSON content */}
        <div className="flex-1 overflow-y-auto -ml-2">
          <JsonViewer data={value} maxDepth={maxDepth} />
        </div>
      </div>
    </TooltipProvider>
  )
}
