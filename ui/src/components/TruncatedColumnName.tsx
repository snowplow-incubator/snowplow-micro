import { SquareAsterisk, SquareCode } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface TruncatedColumnNameProps {
  columnName: string;
  limit?: number;
  className?: string;
  iconSize?: 'sm' | 'md';
}

export function TruncatedColumnName({
  columnName,
  limit = 35,
  className = "",
  iconSize = 'sm'
}: TruncatedColumnNameProps) {
  let result = columnName;
  let icon: 'contexts' | 'unstruct' | null = null;
  const originalLength = columnName.length;

  // Check for contexts_ prefix
  if (result.startsWith('contexts_')) {
    result = result.replace('contexts_', '');
    icon = 'contexts';
  }

  // Check for unstruct_event_ prefix
  if (result.startsWith('unstruct_event_')) {
    result = result.replace('unstruct_event_', '');
    icon = 'unstruct';
  }

  // If still over limit chars, truncate in the middle
  if (result.length > limit) {
    const totalLength = limit;
    const ellipsis = '[...]';
    const availableLength = totalLength - ellipsis.length;
    const startLength = Math.ceil(availableLength / 2);
    const endLength = Math.floor(availableLength / 2);

    const start = result.substring(0, startLength);
    const end = result.substring(result.length - endLength);

    result = `${start}${ellipsis}${end}`;
  }

  const iconClass = iconSize === 'sm' ? 'h-3 w-3' : 'h-4 w-4';
  const wasTruncated = icon !== null || result.length < originalLength;

  const content = (
    <div className={`flex items-center gap-1 ${className}`}>
      {icon === 'contexts' && <SquareAsterisk className={`${iconClass} flex-shrink-0`} />}
      {icon === 'unstruct' && <SquareCode className={`${iconClass} flex-shrink-0`} />}
      <span className="truncate">{result}</span>
    </div>
  );

  if (wasTruncated) {
    return (
      <Tooltip delayDuration={0}>
        <TooltipTrigger asChild>
          {content}
        </TooltipTrigger>
        <TooltipContent>
          {columnName}
        </TooltipContent>
      </Tooltip>
    );
  }

  return content;
}