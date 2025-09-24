import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface TruncatedCellProps {
  value: string;
  maxLength?: number;
}

export function TruncatedCell({ value, maxLength = 40 }: TruncatedCellProps) {
  const stringValue = String(value);
  const needsTruncation = stringValue.length > maxLength;

  let displayValue = stringValue;
  if (needsTruncation) {
    const halfLength = Math.floor((maxLength - 5) / 2); // Reserve 3 chars for "..."
    const start = stringValue.slice(0, halfLength);
    const end = stringValue.slice(-halfLength);
    displayValue = `${start}[...]${end}`;
  }

  if (!needsTruncation) {
    return <div className="px-2 py-1 whitespace-nowrap">{displayValue}</div>;
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <div className="px-2 py-1 cursor-help whitespace-nowrap">
          {displayValue}
        </div>
      </TooltipTrigger>
      <TooltipContent>
        <div className="max-w-md break-all">{stringValue}</div>
      </TooltipContent>
    </Tooltip>
  );
}