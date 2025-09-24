import { SquareAsterisk, SquareCode } from 'lucide-react'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { type ColumnMetadata } from '@/utils/column-metadata'

type TruncatedColumnNameProps = {
  columnMetadata: ColumnMetadata
  className?: string
  iconSize?: 'sm' | 'md'
}

export function TruncatedColumnName({
  columnMetadata,
  className = '',
  iconSize = 'sm',
}: TruncatedColumnNameProps) {
  const { name: columnName, truncatedName, icon } = columnMetadata
  const displayText = truncatedName ?? columnName
  const iconClass = iconSize === 'sm' ? 'h-3 w-3' : 'h-4 w-4'
  const wasTruncated = truncatedName !== undefined

  const content = (
    <div className={`flex items-center gap-1 ${className}`}>
      {icon === 'entity' && (
        <SquareAsterisk className={`${iconClass} flex-shrink-0`} />
      )}
      {icon === 'event' && (
        <SquareCode className={`${iconClass} flex-shrink-0`} />
      )}
      <span className="truncate">{displayText}</span>
    </div>
  )

  if (wasTruncated) {
    return (
      <Tooltip delayDuration={0}>
        <TooltipTrigger asChild>{content}</TooltipTrigger>
        <TooltipContent>{columnName}</TooltipContent>
      </Tooltip>
    )
  }

  return content
}
