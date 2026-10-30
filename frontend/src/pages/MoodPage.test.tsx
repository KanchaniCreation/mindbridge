import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as mood from '../api/mood'
import type { MoodSummary } from '../api/mood'
import MoodPage from './MoodPage'

vi.mock('../api/mood')

function summaryOf(overrides: Partial<MoodSummary> = {}): MoodSummary {
  return {
    period: 'WEEK',
    from: '2026-10-21T00:00:00Z',
    to: '2026-10-28T00:00:00Z',
    timezone: 'UTC',
    entryCount: 2,
    daysWithCheckIn: 2,
    minimumEntries: 5,
    sufficientData: false,
    moodCounts: { '1': 0, '2': 0, '3': 2, '4': 0, '5': 0 },
    topEmotions: [],
    topTags: [],
    averageMood: null,
    firstHalfAverage: null,
    secondHalfAverage: null,
    direction: null,
    daily: [],
    note: 'Calculated from the check-ins you recorded. It describes what you entered, not why.',
    ...overrides,
  }
}

describe('MoodPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(mood.getMoodSummary).mockResolvedValue(summaryOf())
    vi.mocked(mood.listMoods).mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })
  })

  it('saves a check-in with the chosen mood and parsed tags', async () => {
    vi.mocked(mood.createMood).mockResolvedValue({
      id: '1',
      moodValue: 4,
      emotions: [],
      tags: ['Exam Stress', 'sleep'],
      note: null,
      recordedAt: '2026-10-28T10:00:00Z',
    })
    render(<MoodPage />)

    await userEvent.click(screen.getByLabelText('4 Good'))
    await userEvent.type(screen.getByLabelText(/^Tags/), 'Exam Stress, sleep')
    await userEvent.click(screen.getByRole('button', { name: 'Save check-in' }))

    expect(await screen.findByText('Check-in saved.')).toBeInTheDocument()
    expect(mood.createMood).toHaveBeenCalledWith(
      expect.objectContaining({ moodValue: 4, tags: ['Exam Stress', 'sleep'] }),
    )
  })

  it('asks for a mood before saving', async () => {
    render(<MoodPage />)

    await userEvent.click(screen.getByRole('button', { name: 'Save check-in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Choose how you are feeling first.')
    expect(mood.createMood).not.toHaveBeenCalled()
  })

  it('does not show averages when there is too little data', async () => {
    render(<MoodPage />)
    expect(await screen.findByText(/Not enough check-ins yet/)).toBeInTheDocument()
    expect(screen.queryByText(/Average mood/)).not.toBeInTheDocument()
  })

  it('shows the average and direction when there is enough data', async () => {
    vi.mocked(mood.getMoodSummary).mockResolvedValue(
      summaryOf({
        entryCount: 6,
        sufficientData: true,
        averageMood: 3,
        firstHalfAverage: 2,
        secondHalfAverage: 4,
        direction: 'HIGHER',
        daily: [{ date: '2026-10-27', averageMood: 3, entries: 6 }],
      }),
    )
    render(<MoodPage />)

    expect(await screen.findByText(/Average mood: 3\.0 out of 5/)).toBeInTheDocument()
    expect(screen.getByText(/later check-ins averaged higher/)).toBeInTheDocument()
  })
})
