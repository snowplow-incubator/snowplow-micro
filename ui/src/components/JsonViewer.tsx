import { useState } from 'react';
import { ChevronDown, ChevronRight, Copy, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { sortKeys } from '@/utils/sort-keys';

type JsonViewerProps = {
  data: any,
  currentDepth?: number,
  maxDepth?: number,
  keyName?: string | number,
  autoExpand?: boolean
}

export function JsonViewer({ data, currentDepth = 0, maxDepth, keyName, autoExpand = false }: JsonViewerProps) {
  const [isExpanded, setIsExpanded] = useState(maxDepth === undefined || currentDepth < maxDepth || autoExpand);
  const [copied, setCopied] = useState(false);

  const handleToggleExpanded = () => setIsExpanded(!isExpanded)

  const handleCopy = (value: any) => {
    navigator.clipboard.writeText(JSON.stringify(value, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Helper to render key part
  const renderKey = (isClickable = false) => {
    if (keyName === undefined || keyName === null) return null;
    const keyText = typeof keyName === 'string' ? `"${keyName}"` : keyName;

    if (isClickable) {
      return (
        <span
          className="text-gray-600 text-xs font-mono cursor-pointer"
          onClick={handleToggleExpanded}
        >
          {keyText}:
        </span>
      );
    }

    return (
      <span className="text-gray-600 text-xs font-mono">
        {keyText}:
      </span>
    );
  };

  if (data === null) {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="text-muted-foreground text-xs font-mono">null</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={() => handleCopy(null)}
        >
          {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
        </Button>
      </div>
    );
  }

  if (data === undefined) {
    return (
      <div className="inline-flex items-center gap-1">
        {renderKey()}
        <span className="text-muted-foreground text-xs font-mono">undefined</span>
      </div>
    );
  }

  if (typeof data === 'string') {
    return (
      <div className="inline-flex items-start gap-1 group">
        {renderKey()}
        <span className="custom-purple-text text-xs font-mono">"{data}"</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={() => handleCopy(data)}
        >
          {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
        </Button>
      </div>
    );
  }

  if (typeof data === 'number') {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="custom-purple-text text-xs font-mono">{data}</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={() => handleCopy(data)}
        >
          {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
        </Button>
      </div>
    );
  }

  if (typeof data === 'boolean') {
    return (
      <div className="inline-flex items-center gap-1 group">
        {renderKey()}
        <span className="custom-purple-text text-xs font-mono">{data.toString()}</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={() => handleCopy(data)}
        >
          {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
        </Button>
      </div>
    );
  }

  if (Array.isArray(data)) {
    if (data.length === 0) {
      return (
        <div className="inline-flex items-center gap-1 group">
          {renderKey()}
          <span className="text-muted-foreground text-xs font-mono">[]</span>
          <Button
            variant="ghost"
            size="sm"
            className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={() => handleCopy(data)}
          >
            {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
          </Button>
        </div>
      );
    }

    return (
      <div className="flex flex-col">
        <div className="inline-flex items-center gap-1 group">
          {renderKey(true)}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleToggleExpanded}
            className="h-6 px-1 text-xs cursor-pointer"
          >
            {isExpanded ? (
              <ChevronDown className="w-2.5 h-2.5 mr-1" />
            ) : (
              <ChevronRight className="w-2.5 h-2.5 mr-1" />
            )}
            <span className="text-muted-foreground font-mono">[{data.length}]</span>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={() => handleCopy(data)}
          >
            {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
          </Button>
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
    );
  }

  if (typeof data === 'object') {
    const keys = Object.keys(data);
    if (keys.length === 0) {
      return (
        <div className="inline-flex items-center gap-1 group">
          {renderKey()}
          <span className="text-muted-foreground text-xs font-mono">{'{}'}</span>
          <Button
            variant="ghost"
            size="sm"
            className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={() => handleCopy(data)}
          >
            {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
          </Button>
        </div>
      );
    }

    return (
      <div className="flex flex-col">
        <div className="inline-flex items-center gap-1 group">
          {renderKey(true)}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleToggleExpanded}
            className="h-6 px-1 text-xs cursor-pointer"
          >
            {isExpanded ? (
              <ChevronDown className="w-2.5 h-2.5 mr-1" />
            ) : (
              <ChevronRight className="w-2.5 h-2.5 mr-1" />
            )}
            <span className="text-muted-foreground font-mono">
              {`{${keys.length}}`}
            </span>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-4 w-4 p-0 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={() => handleCopy(data)}
          >
            {copied ? <Check className="h-2 w-2" /> : <Copy className="h-2 w-2" />}
          </Button>
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
    );
  }

  return <span className="text-muted-foreground text-xs font-mono">{String(data)}</span>;
}
