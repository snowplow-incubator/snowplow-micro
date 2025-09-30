import { useState, useRef } from 'react'
import { Input } from '@/components/ui/input'
import { truncateMiddle } from '@/utils/event-utils'

type ColumnAutocompleteProps = {
  value: string
  onChange: (value: string) => void
  options: string[]
  placeholder?: string
}

export function ColumnAutocomplete({
  value,
  onChange,
  options,
  placeholder = "Filter...",
}: ColumnAutocompleteProps) {
  const [open, setOpen] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  // Filter options based on search
  const filteredOptions = options.filter((option) =>
    option.toLowerCase().includes(value.toLowerCase())
  )

  const handleInputChange = (newValue: string) => {
    onChange(newValue)
  }

  const handleOptionSelect = (option: string) => {
    onChange(option)
    setOpen(false)
    inputRef.current?.blur()
  }

  return (
    <div className="relative">
      <Input
        ref={inputRef}
        placeholder={placeholder}
        value={value}
        onChange={(e) => handleInputChange(e.target.value)}
        onFocus={() => setOpen(true)}
        onBlur={() => {
          // Use setTimeout to allow clicking on dropdown items
          setTimeout(() => setOpen(false), 200)
        }}
        className="h-8 text-xs font-light"
        style={{ width: '100px' }}
      />

      {open && filteredOptions.length > 0 && (
        <div className="absolute top-full left-0 z-50 w-[300px] mt-1 bg-white border border-gray-200 rounded-md shadow-lg">
          <div className="max-h-60 overflow-auto p-1">
            {filteredOptions.map((option, index) => (
              <div
                key={option || `option-${index}`}
                className="px-2 py-1.5 text-sm font-light cursor-pointer hover:bg-gray-100 rounded-sm"
                onMouseDown={(e) => {
                  e.preventDefault() // Prevent input blur
                  handleOptionSelect(option)
                }}
              >
                {truncateMiddle(option, 50)}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}