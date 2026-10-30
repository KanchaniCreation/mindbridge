import type { DailyMood } from '../api/mood'

const HEIGHT = 180
const PAD = { top: 8, right: 8, bottom: 28, left: 28 }
const BAR_WIDTH = 24

/** Daily average mood (1 to 5) as bars. A table version is included for screen readers. */
export default function MoodChart({ daily }: { daily: DailyMood[] }) {
  if (daily.length === 0) {
    return <p>No check-ins in this period yet.</p>
  }

  const step = 48
  const width = PAD.left + PAD.right + daily.length * step
  const plotHeight = HEIGHT - PAD.top - PAD.bottom
  const yFor = (value: number) => PAD.top + plotHeight - (value / 5) * plotHeight

  return (
    <div className="chart">
      <div className="chart-scroll">
        <svg
          role="img"
          aria-label={`Daily average mood for ${daily.length} days, on a scale of 1 to 5`}
          viewBox={`0 0 ${width} ${HEIGHT}`}
          width={width}
          height={HEIGHT}
        >
          {[1, 2, 3, 4, 5].map((tick) => (
            <g key={tick}>
              <line x1={PAD.left} x2={width - PAD.right} y1={yFor(tick)} y2={yFor(tick)}
                stroke="currentColor" strokeOpacity="0.15" />
              <text x={PAD.left - 6} y={yFor(tick) + 4} textAnchor="end" fontSize="10"
                fill="currentColor">
                {tick}
              </text>
            </g>
          ))}
          {daily.map((day, index) => {
            const x = PAD.left + index * step + (step - BAR_WIDTH) / 2
            const y = yFor(day.averageMood)
            return (
              <g key={day.date}>
                <rect data-day={day.date} x={x} y={y} width={BAR_WIDTH}
                  height={PAD.top + plotHeight - y} fill="currentColor" fillOpacity="0.6">
                  <title>{`${day.date}: average ${day.averageMood} from ${day.entries} check-in(s)`}</title>
                </rect>
                <text x={x + BAR_WIDTH / 2} y={HEIGHT - 10} textAnchor="middle" fontSize="10"
                  fill="currentColor">
                  {day.date.slice(5)}
                </text>
              </g>
            )
          })}
        </svg>
      </div>
      <details>
        <summary>View as a table</summary>
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Average mood</th>
              <th>Check-ins</th>
            </tr>
          </thead>
          <tbody>
            {daily.map((day) => (
              <tr key={day.date}>
                <td>{day.date}</td>
                <td>{day.averageMood}</td>
                <td>{day.entries}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}
