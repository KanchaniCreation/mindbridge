import { apiFetch } from './http'
import type { Page } from './types'

export type ChatStatus = 'ACTIVE' | 'CLOSED'

export interface ChatSummary {
  id: string
  status: ChatStatus
  myPseudonym: string
  partnerPseudonym: string
  createdAt: string
  lastMessageAt: string | null
}

export interface ChatMessage {
  id: string
  mine: boolean
  senderPseudonym: string
  body: string
  createdAt: string
}

export interface Invite {
  code: string
  expiresAt: string
}

export function confirmAdult(): Promise<void> {
  return apiFetch<void>('/chat/adult-confirmation', { method: 'POST' })
}

export function createInvite(): Promise<Invite> {
  return apiFetch<Invite>('/chat/invites', { method: 'POST' })
}

export function redeemInvite(code: string): Promise<ChatSummary> {
  return apiFetch<ChatSummary>('/chat/invites/redeem', { method: 'POST', body: { code } })
}

export function listChats(signal?: AbortSignal): Promise<ChatSummary[]> {
  return apiFetch<ChatSummary[]>('/chats', { signal })
}

/** Newest first, as the server sends them. */
export function getMessages(
  chatId: string,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<Page<ChatMessage>> {
  return apiFetch<Page<ChatMessage>>(`/chats/${chatId}/messages?page=${page}&size=${size}`, { signal })
}

export function sendMessage(chatId: string, clientMessageId: string, body: string): Promise<ChatMessage> {
  return apiFetch<ChatMessage>(`/chats/${chatId}/messages`, {
    method: 'POST',
    body: { clientMessageId, body },
  })
}

export function closeChat(chatId: string): Promise<void> {
  return apiFetch<void>(`/chats/${chatId}/close`, { method: 'POST' })
}

export function reportChat(chatId: string, reason: string, note?: string): Promise<void> {
  return apiFetch<void>(`/chats/${chatId}/report`, {
    method: 'POST',
    body: { reason, note: note || undefined },
  })
}

export interface ChatStreamHandlers {
  onReady?: () => void
  onMessage: (message: ChatMessage) => void
  onClosed: () => void
}

/**
 * Live updates over Server-Sent Events. The browser reconnects by itself if the connection drops;
 * every reconnect sends "ready" again, which is the page's cue to fetch anything it missed.
 * Returns a function that closes the stream.
 */
export function openChatStream(chatId: string, handlers: ChatStreamHandlers): () => void {
  const source = new EventSource(`/api/v1/chats/${chatId}/stream`)

  source.addEventListener('ready', () => handlers.onReady?.())
  source.addEventListener('message', (event) => {
    try {
      handlers.onMessage(JSON.parse((event as MessageEvent<string>).data) as ChatMessage)
    } catch {
      // Ignore an event that is not valid JSON.
    }
  })
  source.addEventListener('closed', () => {
    handlers.onClosed()
    source.close()
  })

  return () => source.close()
}
