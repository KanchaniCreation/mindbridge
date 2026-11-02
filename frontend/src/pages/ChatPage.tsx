import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  closeChat,
  getMessages,
  listChats,
  openChatStream,
  reportChat,
  sendMessage,
  type ChatMessage,
  type ChatSummary,
} from '../api/chat'
import { REPORT_REASONS } from '../api/chatReasons'
import { newClientMessageId } from '../api/clientId'
import { errorMessage } from '../api/errors'
import { ApiError } from '../api/http'
import './chat.css'

const PAGE_SIZE = 30

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; chat: ChatSummary; totalPages: number }

type Panel = 'none' | 'close' | 'report'

/** Adds new messages to the list without duplicates and keeps the oldest first. */
function mergeMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byId = new Map(current.map((m) => [m.id, m]))
  for (const message of incoming) {
    byId.set(message.id, message)
  }
  return [...byId.values()].sort((a, b) => Date.parse(a.createdAt) - Date.parse(b.createdAt))
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ChatPage() {
  const { id = '' } = useParams()
  const [load, setLoad] = useState<LoadState>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [loadedPage, setLoadedPage] = useState(0)
  const [loadingEarlier, setLoadingEarlier] = useState(false)
  const [closed, setClosed] = useState(false)

  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  // Remembers the id of a message that failed, so retrying the same text cannot create a duplicate.
  const pending = useRef<{ id: string; body: string } | null>(null)

  const [panel, setPanel] = useState<Panel>('none')
  const [reason, setReason] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const bottomRef = useRef<HTMLDivElement | null>(null)
  const lastMessageId = messages.length > 0 ? messages[messages.length - 1].id : null

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([listChats(controller.signal), getMessages(id, 0, PAGE_SIZE, controller.signal)])
      .then(([chats, page]) => {
        const chat = chats.find((c) => c.id === id)
        if (!chat) {
          setLoad({ kind: 'error', message: 'Chat not found.' })
          return
        }
        setMessages((current) => mergeMessages(current, page.items))
        setLoadedPage(0)
        setLoad({ kind: 'ready', chat, totalPages: page.totalPages })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setLoad({ kind: 'error', message: errorMessage(error) })
      })
    return () => controller.abort()
  }, [id, attempt])

  useEffect(() => {
    return openChatStream(id, {
      onReady: () => {
        // Also runs after the browser reconnects, so anything missed in between shows up.
        getMessages(id, 0, PAGE_SIZE)
          .then((page) => setMessages((current) => mergeMessages(current, page.items)))
          .catch(() => undefined)
      },
      onMessage: (message) => setMessages((current) => mergeMessages(current, [message])),
      onClosed: () => setClosed(true),
    })
  }, [id])

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ block: 'end' })
  }, [lastMessageId])

  function retryLoad() {
    setLoad({ kind: 'loading' })
    setAttempt((current) => current + 1)
  }

  async function loadEarlier() {
    const next = loadedPage + 1
    setLoadingEarlier(true)
    try {
      const page = await getMessages(id, next, PAGE_SIZE)
      setMessages((current) => mergeMessages(current, page.items))
      setLoadedPage(next)
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setLoadingEarlier(false)
    }
  }

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const body = draft.trim()
    if (!body || sending) return

    const clientMessageId =
      pending.current && pending.current.body === body ? pending.current.id : newClientMessageId()
    pending.current = { id: clientMessageId, body }

    setSending(true)
    setSendError(null)
    try {
      const sent = await sendMessage(id, clientMessageId, body)
      pending.current = null
      setMessages((current) => mergeMessages(current, [sent]))
      setDraft('')
    } catch (error) {
      if (error instanceof ApiError && error.status === 409 && error.message.includes('closed')) {
        setClosed(true)
      }
      setSendError(errorMessage(error))
    } finally {
      setSending(false)
    }
  }

  async function confirmClose() {
    setBusy(true)
    setActionError(null)
    try {
      await closeChat(id)
      setClosed(true)
      setPanel('none')
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  async function submitReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!reason) return
    setBusy(true)
    setActionError(null)
    try {
      await reportChat(id, reason, note.trim())
      setClosed(true)
      setPanel('none')
      setNotice('Report sent. The chat is closed.')
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  if (load.kind === 'loading') {
    return (
      <main className="content">
        <p role="status">Loading chat…</p>
      </main>
    )
  }

  if (load.kind === 'error') {
    return (
      <main className="content">
        <div role="alert">
          <p className="error">{load.message}</p>
          <button onClick={retryLoad}>Retry</button>
        </div>
        <p>
          <Link to="/chats">Back to chats</Link>
        </p>
      </main>
    )
  }

  const chat = load.chat
  const isClosed = closed || chat.status === 'CLOSED'
  const hasEarlier = loadedPage + 1 < load.totalPages

  return (
    <main className="content">
      <div className="chat-header">
        <div>
          <h1>{chat.partnerPseudonym}</h1>
          <p className="note">You are {chat.myPseudonym}</p>
        </div>
        <Link to="/chats">All chats</Link>
      </div>

      {notice && <p role="status">{notice}</p>}

      {hasEarlier && (
        <button onClick={() => void loadEarlier()} disabled={loadingEarlier}>
          Load earlier messages
        </button>
      )}

      {messages.length === 0 && <p>No messages yet. Say hello.</p>}

      <ul className="chat-messages" aria-label="Messages">
        {messages.map((message) => (
          <li key={message.id} className={`chat-message ${message.mine ? 'mine' : 'theirs'}`}>
            <p className="note">
              {message.mine ? 'You' : message.senderPseudonym}, {formatTime(message.createdAt)}
            </p>
            <p>{message.body}</p>
          </li>
        ))}
      </ul>
      <div ref={bottomRef} />

      {isClosed ? (
        <p>This chat is closed. Neither of you can send more messages.</p>
      ) : (
        <form className="chat-composer" onSubmit={(e) => void send(e)}>
          <label htmlFor="chat-message">Message</label>
          <textarea
            id="chat-message"
            value={draft}
            maxLength={2000}
            onChange={(e) => setDraft(e.target.value)}
          />
          {sendError && (
            <p role="alert" className="error">
              {sendError}
            </p>
          )}
          <div>
            <button type="submit" disabled={sending || draft.trim() === ''}>
              Send
            </button>
          </div>
        </form>
      )}

      {actionError && panel === 'none' && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}

      {!isClosed && panel === 'none' && (
        <div className="chat-actions">
          <button onClick={() => setPanel('close')}>Close chat</button>
          <button onClick={() => setPanel('report')}>Report</button>
        </div>
      )}

      {panel === 'close' && (
        <section className="chat-panel" aria-label="Close chat">
          <p>Close this chat? Neither of you can send more messages afterwards.</p>
          {actionError && (
            <p role="alert" className="error">
              {actionError}
            </p>
          )}
          <div className="chat-actions">
            <button onClick={() => void confirmClose()} disabled={busy}>
              Yes, close
            </button>
            <button onClick={() => setPanel('none')} disabled={busy}>
              Cancel
            </button>
          </div>
        </section>
      )}

      {panel === 'report' && (
        <section className="chat-panel" aria-label="Report chat">
          <p>Reporting closes the chat. Only your reason and note are sent, never the messages.</p>
          <form className="form" onSubmit={(e) => void submitReport(e)}>
            <div className="field">
              <label htmlFor="report-reason">Reason</label>
              <select id="report-reason" value={reason} onChange={(e) => setReason(e.target.value)}>
                <option value="">Choose a reason</option>
                {REPORT_REASONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="report-note">Note (optional)</label>
              <textarea
                id="report-note"
                value={note}
                maxLength={500}
                onChange={(e) => setNote(e.target.value)}
              />
            </div>
            {actionError && (
              <p role="alert" className="error">
                {actionError}
              </p>
            )}
            <div className="chat-actions">
              <button type="submit" disabled={busy || reason === ''}>
                Report and close
              </button>
              <button type="button" onClick={() => setPanel('none')} disabled={busy}>
                Cancel
              </button>
            </div>
          </form>
        </section>
      )}
    </main>
  )
}
