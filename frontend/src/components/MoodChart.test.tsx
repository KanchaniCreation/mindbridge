import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import MoodChart from './MoodChart'

describe('MoodChart', () => {
  it('draws one bar per day with an accessible description', () => {
    const { container } = render(
      <MoodChart
        daily={[
          { date: '2026-10-27', averageMood: 3.5, entries: 2 },
          { date: '2026-10-28', averageMood: 4, entries: 1 },
        ]}
      />,
    )

    expect(screen.getByRole('img', { name: /daily average mood for 2 days/i })).toBeInTheDocument()
    expect(container.querySelectorAll('rect[data-day]')).toHaveLength(2)
    expect(screen.getByText('3.5')).toBeInTheDocument()
  })

  it('says so when there are no check-ins', () => {
    render(<MoodChart daily={[]} />)
    expect(screen.getByText('No check-ins in this period yet.')).toBeInTheDocument()
  })
})
