import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  confirmAdult,
  createInvite,
  listChats,
  redeemInvite,
  type ChatSummary,
  type Invite,
} from '../api/chat'
import { errorMessage } from '../api/errors'
import { ApiError } from '../api/http'
import './chat.css'

type ListState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; chats: ChatSummary[] }

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ChatsPage() {
  const navigate = useNavigate()
  const [state, setState] = useState<ListState>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [invite, setInvite] = useState<Invite | null>(null)
  const [codeInput, setCodeInput] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)
  const [needsAdult, setNeedsAdult] = useState(false)
  const [busy, setBusy] = useState(false)
  // The action that was refused because the age confirmation is missing; it runs again after confirming.
  const retryAction = useRef<(() => Promise<void>) | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    listChats(controller.signal)
      .then((chats) => setState({ kind: 'ready', chats }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setState({ kind: 'error', message: errorMessage(error) })
      })
    return () => controller.abort()
  }, [attempt])

  function retryLoad() {
    setState({ kind: 'loading' })
    setAttempt((current) => current + 1)
  }

  async function runAction(action: () => Promise<void>) {
    setBusy(true)
    setActionError(null)
    try {
      await action()
      setNeedsAdult(false)
      retryAction.current = null
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) {
        retryAction.current = action
        setNeedsAdult(true)
      } else {
        setActionError(errorMessage(error))
      }
    } finally {
      setBusy(false)
    }
  }

  function makeInvite() {
    void runAction(async () => {
      setInvite(await createInvite())
    })
  }

  function joinChat(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const code = codeInput.trim()
    if (!code) return
    void runAction(async () => {
      const chat = await redeemInvite(code)
      navigate(`/chats/${chat.id}`)
    })
  }

  async function confirmAgeAndContinue() {
    setBusy(true)
    setActionError(null)
    try {
      await confirmAdult()
    } catch (error) {
      setActionError(errorMessage(error))
      setBusy(false)
      return
    }
    setNeedsAdult(false)
    setBusy(false)
    const action = retryAction.current
    retryAction.current = null
    if (action) await runAction(action)
  }

  return (
    <main className="content">
      <div className="page-title">
        <h1>Chats</h1>
      </div>
      <p className="note">
        You and the other person only see made-up names. Share an invite code to start a chat.
      </p>

      {needsAdult && (
        <section className="chat-panel" aria-label="Age confirmation">
          <p>Chat is for adults. Please confirm that you are 18 or older to use chat.</p>
          <button onClick={() => void confirmAgeAndContinue()} disabled={busy}>
            I am 18 or older
          </button>
        </section>
      )}

      {actionError && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}

      <section className="chat-panel" aria-label="Invite someone">
        <h2>Invite someone</h2>
        <button onClick={makeInvite} disabled={busy}>
          Create invite code
        </button>
        {invite && (
          <div>
            <p>
              Your invite code: <strong className="invite-code">{invite.code}</strong>
            </p>
            <p className="note">
              This code is shown only once and works for one person. It expires{' '}
              {formatDateTime(invite.expiresAt)}.
            </p>
          </div>
        )}
      </section>

      <section className="chat-panel" aria-label="Join with a code">
        <h2>Join with a code</h2>
        <form className="form form-inline" onSubmit={joinChat}>
          <div className="field">
            <label htmlFor="invite-code">Invite code</label>
            <input
              id="invite-code"
              type="text"
              autoComplete="off"
              value={codeInput}
              onChange={(e) => setCodeInput(e.target.value)}
            />
          </div>
          <button type="submit" disabled={busy}>
            Join chat
          </button>
        </form>
      </section>

      <h2>Your chats</h2>
      {state.kind === 'loading' && <p role="status">Loading your chats…</p>}

      {state.kind === 'error' && (
        <div role="alert">
          <p className="error">{state.message}</p>
          <button onClick={retryLoad}>Retry</button>
        </div>
      )}

      {state.kind === 'ready' && state.chats.length === 0 && (
        <p>No chats yet. Create an invite code or join with one.</p>
      )}

      {state.kind === 'ready' && state.chats.length > 0 && (
        <ul className="entry-list">
          {state.chats.map((chat) => (
            <li key={chat.id} className="entry">
              <h3>{chat.partnerPseudonym}</h3>
              <p className="note">
                You are {chat.myPseudonym}
                {chat.status === 'CLOSED' ? ' (closed)' : ''}
                {chat.lastMessageAt ? `, last message ${formatDateTime(chat.lastMessageAt)}` : ''}
              </p>
              <Link to={`/chats/${chat.id}`}>Open chat</Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
