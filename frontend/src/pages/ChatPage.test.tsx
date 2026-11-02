import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as chatApi from '../api/chat'
import type { ChatMessage, ChatStreamHandlers, ChatSummary } from '../api/chat'
import { REPORT_REASONS } from '../api/chatReasons'
import { ApiError } from '../api/http'
import type { Page } from '../api/types'
import ChatPage from './ChatPage'

vi.mock('../api/chat')

const summary: ChatSummary = {
  id: 'c1',
  status: 'ACTIVE',
  myPseudonym: 'Brave Owl',
  partnerPseudonym: 'Quiet Fox',
  createdAt: '2026-11-01T09:00:00Z',
  lastMessageAt: '2026-11-01T10:01:00Z',
}

const older: ChatMessage = {
  id: 'm1',
  mine: false,
  senderPseudonym: 'Quiet Fox',
  body: 'Hi there',
  createdAt: '2026-11-01T10:00:00Z',
}

const newer: ChatMessage = {
  id: 'm2',
  mine: true,
  senderPseudonym: 'Brave Owl',
  body: 'Hello back',
  createdAt: '2026-11-01T10:01:00Z',
}

function pageOf(items: ChatMessage[], totalPages = 1): Page<ChatMessage> {
  return { items, page: 0, size: 30, totalElements: items.length, totalPages }
}

let handlers: ChatStreamHandlers

function renderChat() {
  return render(
    <MemoryRouter initialEntries={['/chats/c1']}>
      <Routes>
        <Route path="/chats/:id" element={<ChatPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ChatPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(chatApi.listChats).mockResolvedValue([summary])
    // The server sends the newest message first.
    vi.mocked(chatApi.getMessages).mockResolvedValue(pageOf([newer, older]))
    vi.mocked(chatApi.openChatStream).mockImplementation((_id, h) => {
      handlers = h
      return () => {}
    })
  })

  it('shows the messages oldest first', async () => {
    renderChat()
    await screen.findByText('Hello back')
    const items = screen.getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Hi there')
    expect(items[1]).toHaveTextContent('Hello back')
  })

  it('shows a prompt when there are no messages yet', async () => {
    vi.mocked(chatApi.getMessages).mockResolvedValue(pageOf([]))
    renderChat()
    expect(await screen.findByText(/No messages yet/)).toBeInTheDocument()
  })

  it('says when the chat cannot be found', async () => {
    vi.mocked(chatApi.listChats).mockResolvedValue([])
    renderChat()
    expect(await screen.findByRole('alert')).toHaveTextContent('Chat not found.')
  })

  it('sends a message and clears the box', async () => {
    vi.mocked(chatApi.sendMessage).mockResolvedValue({
      id: 'm3',
      mine: true,
      senderPseudonym: 'Brave Owl',
      body: 'Hello',
      createdAt: '2026-11-01T10:02:00Z',
    })
    renderChat()
    await screen.findByText('Hello back')

    await userEvent.type(screen.getByLabelText('Message'), 'Hello')
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Hello')).toBeInTheDocument()
    expect(screen.getByLabelText('Message')).toHaveValue('')
    expect(chatApi.sendMessage).toHaveBeenCalledWith('c1', expect.any(String), 'Hello')
  })

  it('reuses the same clientMessageId when a failed send is retried', async () => {
    vi.mocked(chatApi.sendMessage)
      .mockRejectedValueOnce(new ApiError(429, 'You are sending messages too quickly. Please slow down.'))
      .mockResolvedValueOnce({
        id: 'm3',
        mine: true,
        senderPseudonym: 'Brave Owl',
        body: 'Hello',
        createdAt: '2026-11-01T10:02:00Z',
      })
    renderChat()
    await screen.findByText('Hello back')

    await userEvent.type(screen.getByLabelText('Message'), 'Hello')
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('too quickly')

    await userEvent.click(screen.getByRole('button', { name: 'Send' }))
    await screen.findByText('Hello')

    const calls = vi.mocked(chatApi.sendMessage).mock.calls
    expect(calls).toHaveLength(2)
    expect(calls[1][1]).toBe(calls[0][1])
  })

  it('shows a message that arrives live', async () => {
    renderChat()
    await screen.findByText('Hello back')

    act(() =>
      handlers.onMessage({
        id: 'm9',
        mine: false,
        senderPseudonym: 'Quiet Fox',
        body: 'Are you there?',
        createdAt: '2026-11-01T10:05:00Z',
      }),
    )

    expect(await screen.findByText('Are you there?')).toBeInTheDocument()
  })

  it('turns the chat read-only when the other person closes it', async () => {
    renderChat()
    await screen.findByText('Hello back')

    act(() => handlers.onClosed())

    expect(await screen.findByText(/This chat is closed/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Send' })).not.toBeInTheDocument()
  })

  it('closes the chat after you confirm', async () => {
    vi.mocked(chatApi.closeChat).mockResolvedValue(undefined)
    renderChat()
    await screen.findByText('Hello back')

    await userEvent.click(screen.getByRole('button', { name: 'Close chat' }))
    await userEvent.click(screen.getByRole('button', { name: 'Yes, close' }))

    expect(await screen.findByText(/This chat is closed/)).toBeInTheDocument()
    expect(chatApi.closeChat).toHaveBeenCalledWith('c1')
  })

  it('reports the chat with a reason and a note', async () => {
    vi.mocked(chatApi.reportChat).mockResolvedValue(undefined)
    renderChat()
    await screen.findByText('Hello back')

    await userEvent.click(screen.getByRole('button', { name: 'Report' }))
    await userEvent.selectOptions(screen.getByLabelText('Reason'), REPORT_REASONS[0].value)
    await userEvent.type(screen.getByLabelText('Note (optional)'), 'rude')
    await userEvent.click(screen.getByRole('button', { name: 'Report and close' }))

    expect(await screen.findByText(/Report sent/)).toBeInTheDocument()
    expect(chatApi.reportChat).toHaveBeenCalledWith('c1', REPORT_REASONS[0].value, 'rude')
  })

  it('loads earlier messages on request', async () => {
    vi.mocked(chatApi.getMessages)
      .mockResolvedValueOnce(pageOf([newer], 2))
      .mockResolvedValueOnce(pageOf([older], 2))
    renderChat()
    await screen.findByText('Hello back')

    await userEvent.click(screen.getByRole('button', { name: 'Load earlier messages' }))

    expect(await screen.findByText('Hi there')).toBeInTheDocument()
    expect(chatApi.getMessages).toHaveBeenLastCalledWith('c1', 1, 30)
  })
})
