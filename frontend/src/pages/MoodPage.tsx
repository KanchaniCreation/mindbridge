import { useEffect, useState, type FormEvent } from 'react'
import { errorMessage } from '../api/errors'
import {
  createMood,
  deleteMood,
  getMoodSummary,
  listMoods,
  type MoodEntry,
  type MoodPeriod,
  type MoodSummary,
} from '../api/mood'
import type { Page } from '../api/types'
import MoodChart from '../components/MoodChart'

const TIMEZONE = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'

const MOOD_OPTIONS = [
  { value: 1, label: 'Very low' },
  { value: 2, label: 'Low' },
  { value: 3, label: 'Okay' },
  { value: 4, label: 'Good' },
  { value: 5, label: 'Very good' },
]

type Loadable<T> = { kind: 'loading' } | { kind: 'error'; message: string } | { kind: 'ready'; data: T }

function parseList(text: string): string[] {
  return text
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

function describeDirection(summary: MoodSummary): string | null {
  if (!summary.direction || summary.firstHalfAverage === null || summary.secondHalfAverage === null) {
    return null
  }
  const pair = `${summary.firstHalfAverage} then ${summary.secondHalfAverage}`
  switch (summary.direction) {
    case 'HIGHER':
      return `Your later check-ins averaged higher than your earlier ones (${pair}).`
    case 'LOWER':
      return `Your later check-ins averaged lower than your earlier ones (${pair}).`
    default:
      return `Your earlier and later check-ins averaged about the same (${pair}).`
  }
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export default function MoodPage() {
  const [mood, setMood] = useState<number | null>(null)
  const [emotionsText, setEmotionsText] = useState('')
  const [tagsText, setTagsText] = useState('')
  const [note, setNote] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [savedMessage, setSavedMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [period, setPeriod] = useState<MoodPeriod>('WEEK')
  const [reload, setReload] = useState(0)
  const [summary, setSummary] = useState<Loadable<MoodSummary>>({ kind: 'loading' })
  const [history, setHistory] = useState<Loadable<Page<MoodEntry>>>({ kind: 'loading' })
  const [pendingDelete, setPendingDelete] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getMoodSummary(period, TIMEZONE, controller.signal)
      .then((data) => setSummary({ kind: 'ready', data }))
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setSummary({ kind: 'error', message: errorMessage(e) })
      })
    return () => controller.abort()
  }, [period, reload])

  useEffect(() => {
    const controller = new AbortController()
    listMoods({ page: 0, size: 10 }, controller.signal)
      .then((data) => setHistory({ kind: 'ready', data }))
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setHistory({ kind: 'error', message: errorMessage(e) })
      })
    return () => controller.abort()
  }, [reload])

  function changePeriod(next: MoodPeriod) {
    setSummary({ kind: 'loading' })
    setPeriod(next)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)
    setSavedMessage(null)

    if (mood === null) {
      setFormError('Choose how you are feeling first.')
      return
    }
    const emotions = parseList(emotionsText)
    const tags = parseList(tagsText)
    if (emotions.length > 5) {
      setFormError('Choose at most 5 emotions.')
      return
    }
    if (tags.length > 8) {
      setFormError('Use at most 8 tags.')
      return
    }

    setSaving(true)
    try {
      await createMood({ moodValue: mood, emotions, tags, note: note.trim() || undefined })
      setMood(null)
      setEmotionsText('')
      setTagsText('')
      setNote('')
      setSavedMessage('Check-in saved.')
      setReload((n) => n + 1)
    } catch (e) {
      setFormError(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  async function confirmDelete(id: string) {
    try {
      await deleteMood(id)
      setPendingDelete(null)
      setReload((n) => n + 1)
    } catch (e) {
      setPendingDelete(null)
      setFormError(errorMessage(e))
    }
  }

  return (
    <main className="content">
      <h1>Mood</h1>

      <form className="form" onSubmit={handleSubmit} noValidate>
        <fieldset>
          <legend>How are you feeling?</legend>
          <div className="mood-options">
            {MOOD_OPTIONS.map((option) => (
              <label key={option.value} className="checkbox">
                <input type="radio" name="mood" value={option.value}
                  checked={mood === option.value} onChange={() => setMood(option.value)} />
                <span>{`${option.value} ${option.label}`}</span>
              </label>
            ))}
          </div>
        </fieldset>
        <div className="field">
          <label htmlFor="emotions">Emotions (optional, comma separated, up to 5)</label>
          <input id="emotions" type="text" value={emotionsText}
            onChange={(e) => setEmotionsText(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="tags">Tags (optional, comma separated, up to 8)</label>
          <input id="tags" type="text" value={tagsText} onChange={(e) => setTagsText(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="note">Note (optional)</label>
          <textarea id="note" rows={3} maxLength={1000} value={note}
            onChange={(e) => setNote(e.target.value)} />
        </div>
        {formError && (
          <p role="alert" className="error">
            {formError}
          </p>
        )}
        {savedMessage && <p role="status">{savedMessage}</p>}
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save check-in'}
        </button>
      </form>

      <section aria-labelledby="summary-heading">
        <h2 id="summary-heading">Your summary</h2>
        <div className="field">
          <label htmlFor="period">Period</label>
          <select id="period" value={period} onChange={(e) => changePeriod(e.target.value as MoodPeriod)}>
            <option value="WEEK">Last 7 days</option>
            <option value="MONTH">Last 30 days</option>
          </select>
        </div>

        {summary.kind === 'loading' && <p role="status">Loading your summary…</p>}
        {summary.kind === 'error' && (
          <p role="alert" className="error">
            {summary.message}
          </p>
        )}
        {summary.kind === 'ready' && !summary.data.sufficientData && (
          <p>
            {`Not enough check-ins yet to show averages. You have ${summary.data.entryCount} in this period; at least ${summary.data.minimumEntries} are needed.`}
          </p>
        )}
        {summary.kind === 'ready' && summary.data.sufficientData && (
          <>
            <p>{`Average mood: ${summary.data.averageMood?.toFixed(1) ?? '-'} out of 5`}</p>
            {describeDirection(summary.data) && <p>{describeDirection(summary.data)}</p>}
            {summary.data.topTags.length > 0 && (
              <p>
                {`Tags you used most: ${summary.data.topTags.map((t) => `${t.name} (${t.count})`).join(', ')}.`}
              </p>
            )}
            <p className="note">{`${summary.data.note} Tags and moods that appear together do not show that one causes the other.`}</p>
          </>
        )}
        {summary.kind === 'ready' && <MoodChart daily={summary.data.daily} />}
      </section>

      <section aria-labelledby="history-heading">
        <h2 id="history-heading">Recent check-ins</h2>
        {history.kind === 'loading' && <p role="status">Loading…</p>}
        {history.kind === 'error' && (
          <p role="alert" className="error">
            {history.message}
          </p>
        )}
        {history.kind === 'ready' && history.data.items.length === 0 && (
          <p>No check-ins yet. Your first one will show up here.</p>
        )}
        {history.kind === 'ready' && history.data.items.length > 0 && (
          <ul className="entry-list">
            {history.data.items.map((entry) => (
              <li key={entry.id} className="entry">
                <p>
                  <strong>{`Mood ${entry.moodValue} of 5`}</strong>
                  {` · ${formatDateTime(entry.recordedAt)}`}
                </p>
                {entry.emotions.length > 0 && <p>{`Emotions: ${entry.emotions.join(', ')}`}</p>}
                {entry.tags.length > 0 && <p>{`Tags: ${entry.tags.join(', ')}`}</p>}
                {entry.note && <p>{entry.note}</p>}
                {pendingDelete === entry.id ? (
                  <div role="group" aria-label="Confirm delete" className="confirm">
                    <span>Delete this check-in?</span>
                    <button type="button" className="danger" onClick={() => void confirmDelete(entry.id)}>
                      Yes, delete
                    </button>
                    <button type="button" onClick={() => setPendingDelete(null)}>
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button type="button" onClick={() => setPendingDelete(entry.id)}>
                    Delete
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
