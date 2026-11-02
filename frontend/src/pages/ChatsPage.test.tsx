import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as chatApi from '../api/chat'
import type { ChatSummary } from '../api/chat'
import { ApiError } from '../api/http'
import ChatsPage from './ChatsPage'

vi.mock('../api/chat')

const chat: ChatSummary = {
  id: 'c1',
  status: 'ACTIVE',
  myPseudonym: 'Brave Owl',
  partnerPseudonym: 'Quiet Fox',
  createdAt: '2026-11-01T10:00:00Z',
  lastMessageAt: null,
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/chats']}>
      <Routes>
        <Route path="/chats" element={<ChatsPage />} />
        <Route path="/chats/:id" element={<p>Chat window opened</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ChatsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists your chats by the partner pseudonym', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([chat])
    renderPage()
    expect(await screen.findByText('Quiet Fox')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open chat' })).toHaveAttribute('href', '/chats/c1')
  })

  it('shows a friendly empty state', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    renderPage()
    expect(await screen.findByText(/No chats yet/)).toBeInTheDocument()
  })

  it('shows an error and recovers on retry', async () => {
    vi.mocked(chatApi.listChats)
      .mockRejectedValueOnce(new ApiError(500, 'Server error'))
      .mockResolvedValueOnce([chat])
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Quiet Fox')).toBeInTheDocument()
  })

  it('creates an invite code and shows it', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    vi.mocked(chatApi.createInvite).mockResolvedValue({
      code: 'ABCD-EFGH-JKLM',
      expiresAt: '2026-11-04T10:00:00Z',
    })
    renderPage()
    await screen.findByText(/No chats yet/)

    await userEvent.click(screen.getByRole('button', { name: 'Create invite code' }))
    expect(await screen.findByText('ABCD-EFGH-JKLM')).toBeInTheDocument()
  })

  it('opens the new chat after joining with a code', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    vi.mocked(chatApi.redeemInvite).mockResolvedValue(chat)
    renderPage()
    await screen.findByText(/No chats yet/)

    await userEvent.type(screen.getByLabelText('Invite code'), 'ABCD-EFGH-JKLM')
    await userEvent.click(screen.getByRole('button', { name: 'Join chat' }))

    expect(await screen.findByText('Chat window opened')).toBeInTheDocument()
    expect(chatApi.redeemInvite).toHaveBeenCalledWith('ABCD-EFGH-JKLM')
  })

  it('shows the server message when the code is not valid', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    vi.mocked(chatApi.redeemInvite).mockRejectedValue(
      new ApiError(404, 'That invite code is not valid or has expired.'),
    )
    renderPage()
    await screen.findByText(/No chats yet/)

    await userEvent.type(screen.getByLabelText('Invite code'), 'WRONG')
    await userEvent.click(screen.getByRole('button', { name: 'Join chat' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('not valid or has expired')
  })

  it('asks for the 18+ confirmation, then repeats the action', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    vi.mocked(chatApi.createInvite)
      .mockRejectedValueOnce(new ApiError(403, 'Please confirm that you are 18 or older to use chat.'))
      .mockResolvedValueOnce({ code: 'WXYZ-1234-ABCD', expiresAt: '2026-11-04T10:00:00Z' })
    vi.mocked(chatApi.confirmAdult).mockResolvedValue(undefined)
    renderPage()
    await screen.findByText(/No chats yet/)

    await userEvent.click(screen.getByRole('button', { name: 'Create invite code' }))
    await userEvent.click(await screen.findByRole('button', { name: 'I am 18 or older' }))

    expect(await screen.findByText('WXYZ-1234-ABCD')).toBeInTheDocument()
    expect(chatApi.confirmAdult).toHaveBeenCalledTimes(1)
    expect(chatApi.createInvite).toHaveBeenCalledTimes(2)
  })
})
