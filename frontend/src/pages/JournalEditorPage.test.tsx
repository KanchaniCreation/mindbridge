import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as journal from '../api/journal'
import JournalEditorPage from './JournalEditorPage'

vi.mock('../api/journal')

const savedEntry = {
  id: 'abc',
  title: 'Existing',
  category: 'study',
  body: 'Existing text',
  createdAt: '2026-10-28T10:00:00Z',
  updatedAt: '2026-10-28T10:00:00Z',
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/journal/new" element={<JournalEditorPage />} />
        <Route path="/journal/:id" element={<JournalEditorPage />} />
        <Route path="/journal" element={<p>Journal list</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('JournalEditorPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(journal.getPrompts).mockResolvedValue(['What is on your mind right now?'])
  })

  it('saves a new entry and returns to the list', async () => {
    vi.mocked(journal.createJournal).mockResolvedValue(savedEntry)
    renderAt('/journal/new')

    await userEvent.type(await screen.findByLabelText('Title (optional)'), 'My day')
    await userEvent.type(screen.getByLabelText('Entry'), 'Went well')
    await userEvent.click(screen.getByRole('button', { name: 'Save entry' }))

    await waitFor(() =>
      expect(journal.createJournal).toHaveBeenCalledWith(
        expect.objectContaining({ title: 'My day', body: 'Went well' }),
      ),
    )
    expect(await screen.findByText('Journal list')).toBeInTheDocument()
  })

  it('does not save an empty entry', async () => {
    renderAt('/journal/new')

    await userEvent.click(await screen.findByRole('button', { name: 'Save entry' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Write something before saving.')
    expect(journal.createJournal).not.toHaveBeenCalled()
  })

  it('shows a gentle writing prompt', async () => {
    renderAt('/journal/new')
    expect(await screen.findByText('What is on your mind right now?')).toBeInTheDocument()
  })

  it('loads an entry for editing and asks before deleting it', async () => {
    vi.mocked(journal.getJournal).mockResolvedValue(savedEntry)
    vi.mocked(journal.deleteJournal).mockResolvedValue(undefined)
    renderAt('/journal/abc')

    expect(await screen.findByLabelText('Entry')).toHaveValue('Existing text')

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    expect(screen.getByText('Delete this entry permanently?')).toBeInTheDocument()
    expect(journal.deleteJournal).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: 'Yes, delete' }))
    await waitFor(() => expect(journal.deleteJournal).toHaveBeenCalledWith('abc'))
  })
})
