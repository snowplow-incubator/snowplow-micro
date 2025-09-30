import { useMemo, useState } from 'react'
import { Bar, BarChart, XAxis, Cell } from 'recharts'
import {
  type ChartConfig,
  ChartContainer,
  ChartTooltip,
} from '@/components/ui/chart'
import { type Event } from '@/services/api'
import { hasFailureData, roundToMinute } from '@/utils/event-utils'

interface EventsChartProps {
  events: Event[]
  selectedMinute: string | null
  onMinuteClick: (minute: string | null) => void
}

type ChartData = {
  minute: string
  validEvents: number
  failedEvents: number
  timestamp: number
}

const chartConfig = {
  validEvents: {
    label: 'Valid Events',
    color: 'rgb(var(--dark-teal))',
  },
  failedEvents: {
    label: 'Failed Events',
    color: 'rgb(var(--dark-orange))',
  },
} satisfies ChartConfig

export function EventsChart({
  events,
  selectedMinute,
  onMinuteClick,
}: EventsChartProps) {
  const [barHovered, setBarHovered] = useState(false)

  const chartData = useMemo(() => {
    // Group events by minute
    const minuteGroups = new Map<
      string,
      { valid: number; failed: number; timestamp: number }
    >()

    events.forEach((event) => {
      if (!event.collector_tstamp) return

      const timestamp = new Date(event.collector_tstamp).getTime()
      const roundedTimestamp = roundToMinute(timestamp)
      const minuteKey = new Date(roundedTimestamp).toISOString()

      const hasFailures = hasFailureData(event)

      if (!minuteGroups.has(minuteKey)) {
        minuteGroups.set(minuteKey, {
          valid: 0,
          failed: 0,
          timestamp: roundedTimestamp,
        })
      }

      const group = minuteGroups.get(minuteKey)!
      if (hasFailures) {
        group.failed++
      } else {
        group.valid++
      }
    })

    // Create array with last 30 minutes from the latest event, filling gaps with zero
    const latestEventTime = events.length > 0
      ? Math.max(...events.map(event =>
          event.collector_tstamp ? new Date(event.collector_tstamp).getTime() : 0
        ))
      : Date.now()

    const endTime = roundToMinute(latestEventTime)
    const startTime = endTime - 30 * 60 * 1000
    const data: ChartData[] = []

    for (let time = startTime; time <= endTime; time += 60000) {
      const roundedTime = roundToMinute(time)
      const minuteKey = new Date(roundedTime).toISOString()
      const group = minuteGroups.get(minuteKey)

      data.push({
        minute: new Date(roundedTime).toLocaleTimeString('en-US', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
        }),
        validEvents: group?.valid || 0,
        failedEvents: group?.failed || 0,
        timestamp: roundedTime,
      })
    }

    return data
  }, [events])

  const handleChartClick = (event: any) => {
    if (event && event.activePayload && event.activePayload.length > 0) {
      const data = event.activePayload[0].payload

      // Check if we actually have events at this time point
      if (data.validEvents > 0 || data.failedEvents > 0) {
        // Clicking on a bar with actual data - toggle selection
        const minuteKey = new Date(data.timestamp).toISOString()
        onMinuteClick(selectedMinute === minuteKey ? null : minuteKey)
      } else {
        // Clicking on empty area (no events at this time) - reset selection
        onMinuteClick(null)
      }
    } else {
      // Clicking on empty area - reset selection
      onMinuteClick(null)
    }
  }

  const getBarOpacity = (index: number): number => {
    const data = chartData[index]
    const minuteKey = new Date(data.timestamp).toISOString()
    if (selectedMinute === minuteKey) return 1.0
    if (selectedMinute && selectedMinute !== minuteKey) return 0.4
    return 0.8
  }

  return (
    <div className="h-[100px] w-full rounded-md border">
      <ChartContainer config={chartConfig} className="h-full w-full">
        <BarChart
          height={100}
          data={chartData}
          onClick={handleChartClick}
          margin={{ top: 10, right: 10, left: 10, bottom: 0 }}
        >
          <defs>
            <pattern
              id="failedPattern"
              patternUnits="userSpaceOnUse"
              width="6"
              height="6"
            >
              <rect width="6" height="6" fill="var(--color-failure)" />
              <line
                x1="0"
                y1="0"
                x2="6"
                y2="6"
                stroke="var(--color-failure-dark)"
                strokeWidth="0.5"
                opacity="0.6"
              />
              <line
                x1="0"
                y1="6"
                x2="6"
                y2="0"
                stroke="var(--color-failure-dark)"
                strokeWidth="0.5"
                opacity="0.6"
              />
            </pattern>
          </defs>
          <XAxis
            dataKey="minute"
            axisLine={false}
            tickLine={false}
            fontSize={12}
          />
          <ChartTooltip
            content={({ active, payload }) => {
              if (active && payload && payload.length > 0 && barHovered) {
                const data = payload[0].payload
                return (
                  <div className="bg-gray-900 text-white px-3 py-2 rounded shadow-lg text-sm">
                    <div className="font-medium mb-1">
                      {new Date(data.timestamp).toLocaleString()}
                    </div>
                    <div className="space-y-1">
                      {data.validEvents > 0 && (
                        <div>Valid Events: {data.validEvents}</div>
                      )}
                      {data.failedEvents > 0 && (
                        <div>Failed Events: {data.failedEvents}</div>
                      )}
                    </div>
                    <div className="text-xs text-gray-300 mt-2 border-t border-gray-600 pt-1">
                      Click to toggle filter
                    </div>
                  </div>
                )
              }
              return null
            }}
            cursor={false}
            animationDuration={0}
            isAnimationActive={false}
          />
          <Bar
            dataKey="validEvents"
            stackId="events"
            fill="var(--color-success)"
            onMouseEnter={() => setBarHovered(true)}
            onMouseLeave={() => setBarHovered(false)}
            className="cursor-pointer"
            isAnimationActive={false}
          >
            {chartData.map((entry, index) => {
              const hasFailedEvents = entry.failedEvents > 0
              const radius = hasFailedEvents ? [0, 0, 2, 2] : [2, 2, 2, 2]
              return (
                <Cell
                  key={`valid-cell-${index}`}
                  fillOpacity={getBarOpacity(index)}
                  // @ts-ignore https://github.com/recharts/recharts/issues/3325
                  radius={radius}
                />
              )
            })}
          </Bar>
          <Bar
            dataKey="failedEvents"
            stackId="events"
            fill="url(#failedPattern)"
            onMouseEnter={() => setBarHovered(true)}
            onMouseLeave={() => setBarHovered(false)}
            className="cursor-pointer"
            isAnimationActive={false}
          >
            {chartData.map((entry, index) => {
              const hasValidEvents = entry.validEvents > 0
              const radius = hasValidEvents ? [2, 2, 0, 0] : [2, 2, 2, 2]
              return (
                <Cell
                  key={`failed-cell-${index}`}
                  fillOpacity={getBarOpacity(index)}
                  // @ts-ignore https://github.com/recharts/recharts/issues/3325
                  radius={radius}
                />
              )
            })}
          </Bar>
        </BarChart>
      </ChartContainer>
    </div>
  )
}
