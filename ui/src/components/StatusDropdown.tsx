import { useState, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { ChevronDown } from 'lucide-react'

type StatusDropdownProps = {
  value: 'all' | 'valid' | 'failed'
  onChange: (value: 'all' | 'valid' | 'failed') => void
}

const statusOptions = [
  { value: 'all' as const, label: 'All events' },
  { value: 'valid' as const, label: 'Valid only' },
  { value: 'failed' as const, label: 'Failed only' },
]

export function StatusDropdown({ value, onChange }: StatusDropdownProps) {
  const [open, setOpen] = useState(false)
  const buttonRef = useRef<HTMLButtonElement>(null)

  const selectedOption = statusOptions.find(option => option.value === value) || statusOptions[0]

  const handleOptionSelect = (optionValue: 'all' | 'valid' | 'failed') => {
    onChange(optionValue)
    setOpen(false)
    buttonRef.current?.blur()
  }

  return (
    <div className="flex justify-center">
      <div className="relative">
        <Button
          ref={buttonRef}
          variant="outline"
          size="sm"
          onClick={() => setOpen(!open)}
          onBlur={() => setOpen(false)}
          className="text-xs font-light h-8 justify-between w-24"
        >
          {selectedOption.label}
          <ChevronDown className="h-3 w-3 ml-1" />
        </Button>

        {open && (
          <div className="absolute top-full left-0 z-50 mt-1 bg-white border rounded-md shadow-lg whitespace-nowrap">
            <div className="p-1 flex flex-col">
              {statusOptions.map((option) => (
                <button
                  key={option.value}
                  className="px-2 py-1.5 text-xs font-light text-left hover:bg-gray-100 rounded-sm"
                  onMouseDown={(e) => {
                    e.preventDefault() // Prevent button blur
                    handleOptionSelect(option.value)
                  }}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}