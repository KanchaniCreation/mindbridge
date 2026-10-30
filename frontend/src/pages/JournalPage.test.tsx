import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/http'
import * as journal from '../api/journal'
import type { JournalListItem } from '../api/journal'
import type { Page } from '../api/types'
import JournalPage from './JournalPage'

vi.mock('../api/journal')

const item: JournalListItem = {
  id: '1',
  title: 'Exam week',
  category: 'study',
  excerpt: 'I feel stressed about my exam',
  createdAt: '2026-10-28T10:00:00Z',
  updatedAt: '2026-10-28T10:00:00Z',
}

function pageOf(items: JournalListItem[]): Page<JournalListItem> {
  return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1 }
}

function renderPage() {
  return render(
    <MemoryRouter>
      <JournalPage />
    </MemoryRouter>,
  )
}

describe('JournalPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists the entries returned by the server', async () => {
    vi.mocked(journal.listJournal).mockResolvedValue(pageOf([item]))
    renderPage()
    expect(await screen.findByText('Exam week')).toBeInTheDocument()
    expect(screen.getByText('I feel stressed about my exam')).toBeInTheDocument()
  })

  it('shows a friendly empty state', async () => {
    vi.mocked(journal.listJournal).mockResolvedValue(pageOf([]))
    renderPage()
    expect(await screen.findByText(/No entries yet/)).toBeInTheDocument()
  })

  it('sends the search text and category to the server', async () => {
    vi.mocked(journal.listJournal).mockResolvedValue(pageOf([]))
    renderPage()
    await screen.findByText(/No entries yet/)

    await userEvent.type(screen.getByLabelText('Search'), 'exam')
    await userEvent.type(screen.getByLabelText('Category'), 'study')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() =>
      expect(journal.listJournal).toHaveBeenLastCalledWith(
        expect.objectContaining({ q: 'exam', category: 'study', page: 0 }),
        expect.anything(),
      ),
    )
  })

  it('shows an error and recovers on retry', async () => {
    vi.mocked(journal.listJournal)
      .mockRejectedValueOnce(new ApiError(500, 'Server error'))
      .mockResolvedValueOnce(pageOf([item]))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Exam week')).toBeInTheDocument()
  })
})
