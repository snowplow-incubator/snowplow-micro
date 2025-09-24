import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { truncateMiddle } from '@/utils/event-utils'

interface TruncatedCellProps {
  value: string
  maxLength?: number
}

export function TruncatedCell({ value }: TruncatedCellProps) {
  const maxLength = 40
  const stringValue = String(value)
  const needsTruncation = stringValue.length > maxLength

  const displayValue = needsTruncation
    ? truncateMiddle(stringValue, maxLength)
    : stringValue

  const cellClass = 'px-2 py-1 whitespace-nowrap'

  if (!needsTruncation) {
    return <div className={cellClass}>{displayValue}</div>
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <div className={`${cellClass} cursor-help`}>{displayValue}</div>
      </TooltipTrigger>
      <TooltipContent>
        <div className="max-w-md break-all">{stringValue}</div>
      </TooltipContent>
    </Tooltip>
  )
}
