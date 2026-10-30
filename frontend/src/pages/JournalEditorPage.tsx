import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../api/errors'
import { createJournal, deleteJournal, getJournal, getPrompts, updateJournal } from '../api/journal'

function EditorForm({ id }: { id: string | undefined }) {
  const navigate = useNavigate()
  const [loadState, setLoadState] = useState<'loading' | 'ready' | 'error'>(id ? 'loading' : 'ready')
  const [loadError, setLoadError] = useState('')
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [body, setBody] = useState('')
  const [prompts, setPrompts] = useState<string[]>([])
  const [promptIndex, setPromptIndex] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  useEffect(() => {
    if (!id) return
    const controller = new AbortController()
    getJournal(id, controller.signal)
      .then((entry) => {
        if (controller.signal.aborted) return
        setTitle(entry.title ?? '')
        setCategory(entry.category ?? '')
        setBody(entry.body)
        setLoadState('ready')
      })
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(errorMessage(e))
        setLoadState('error')
      })
    return () => controller.abort()
  }, [id])

  useEffect(() => {
    const controller = new AbortController()
    getPrompts(controller.signal)
      .then((list) => {
        if (!controller.signal.aborted) setPrompts(list)
      })
      .catch(() => undefined)
    return () => controller.abort()
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    if (body.trim() === '') {
      setError('Write something before saving.')
      return
    }
    const input = {
      title: title.trim() || undefined,
      category: category.trim() || undefined,
      body,
    }
    setSaving(true)
    try {
      if (id) {
        await updateJournal(id, input)
      } else {
        await createJournal(input)
      }
      navigate('/journal')
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!id) return
    setSaving(true)
    try {
      await deleteJournal(id)
      navigate('/journal')
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
      setConfirmingDelete(false)
    }
  }

  if (loadState === 'loading') {
    return <p role="status">Loading…</p>
  }
  if (loadState === 'error') {
    return (
      <div role="alert">
        <p className="error">{loadError}</p>
        <Link to="/journal">Back to the journal</Link>
      </div>
    )
  }

  return (
    <>
      <h1>{id ? 'Edit entry' : 'New entry'}</h1>

      {prompts.length > 0 && (
        <div className="prompt">
          <p>{prompts[promptIndex % prompts.length]}</p>
          <button type="button" onClick={() => setPromptIndex((n) => n + 1)}>
            Another prompt
          </button>
          <p className="note">Prompts are only ideas. Write about anything you like.</p>
        </div>
      )}

      <form className="form" onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="title">Title (optional)</label>
          <input id="title" type="text" maxLength={120} value={title}
            onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="category">Category (optional)</label>
          <input id="category" type="text" maxLength={40} value={category}
            onChange={(e) => setCategory(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="body">Entry</label>
          <textarea id="body" rows={12} maxLength={10000} value={body}
            onChange={(e) => setBody(e.target.value)} />
          <span className="note">{`${body.length} / 10000`}</span>
        </div>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <div className="actions">
          <button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save entry'}
          </button>
          <Link to="/journal">Cancel</Link>
        </div>
      </form>

      {id && !confirmingDelete && (
        <button type="button" className="danger" onClick={() => setConfirmingDelete(true)}>
          Delete
        </button>
      )}
      {id && confirmingDelete && (
        <div role="group" aria-label="Confirm delete" className="confirm">
          <p>Delete this entry permanently?</p>
          <button type="button" className="danger" disabled={saving} onClick={() => void handleDelete()}>
            Yes, delete
          </button>
          <button type="button" onClick={() => setConfirmingDelete(false)}>
            Cancel
          </button>
        </div>
      )}
    </>
  )
}

export default function JournalEditorPage() {
  const { id } = useParams()
  return (
    <main className="content">
      <EditorForm key={id ?? 'new'} id={id} />
    </main>
  )
}
