import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api/errors'
import { listJournal, type JournalListItem } from '../api/journal'
import type { Page } from '../api/types'

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; data: Page<JournalListItem> }

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function JournalPage() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [queryInput, setQueryInput] = useState('')
  const [categoryInput, setCategoryInput] = useState('')
  const [filters, setFilters] = useState({ q: '', category: '', page: 0, attempt: 0 })

  useEffect(() => {
    const controller = new AbortController()
    listJournal({ q: filters.q, category: filters.category, page: filters.page }, controller.signal)
      .then((data) => setState({ kind: 'ready', data }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setState({ kind: 'error', message: errorMessage(error) })
      })
    return () => controller.abort()
  }, [filters])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setState({ kind: 'loading' })
    setFilters({ q: queryInput.trim(), category: categoryInput.trim(), page: 0, attempt: 0 })
  }

  function goToPage(page: number) {
    setState({ kind: 'loading' })
    setFilters((current) => ({ ...current, page }))
  }

  function retry() {
    setState({ kind: 'loading' })
    setFilters((current) => ({ ...current, attempt: current.attempt + 1 }))
  }

  return (
    <main className="content">
      <div className="page-title">
        <h1>Journal</h1>
        <Link to="/journal/new">New entry</Link>
      </div>

      <form className="form form-inline" onSubmit={applyFilters}>
        <div className="field">
          <label htmlFor="search">Search</label>
          <input id="search" type="search" value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="category-filter">Category</label>
          <input id="category-filter" type="text" value={categoryInput}
            onChange={(e) => setCategoryInput(e.target.value)} />
        </div>
        <button type="submit">Search</button>
      </form>

      {state.kind === 'loading' && <p role="status">Loading your entries…</p>}

      {state.kind === 'error' && (
        <div role="alert">
          <p className="error">{state.message}</p>
          <button onClick={retry}>Retry</button>
        </div>
      )}

      {state.kind === 'ready' && state.data.items.length === 0 && (
        <p>
          {filters.q || filters.category
            ? 'No entries match your search.'
            : 'No entries yet. Write your first one whenever you like.'}
        </p>
      )}

      {state.kind === 'ready' && state.data.items.length > 0 && (
        <>
          <ul className="entry-list">
            {state.data.items.map((item) => (
              <li key={item.id} className="entry">
                <h2>{item.title ?? 'Untitled'}</h2>
                <p className="note">
                  {formatDate(item.createdAt)}
                  {item.category ? ` · ${item.category}` : ''}
                </p>
                <p>{item.excerpt}</p>
                <Link to={`/journal/${item.id}`}>Open</Link>
              </li>
            ))}
          </ul>
          <div className="pager">
            <button disabled={state.data.page === 0} onClick={() => goToPage(state.data.page - 1)}>
              Previous
            </button>
            <span>{`Page ${state.data.page + 1} of ${Math.max(state.data.totalPages, 1)}`}</span>
            <button
              disabled={state.data.page + 1 >= state.data.totalPages}
              onClick={() => goToPage(state.data.page + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </main>
  )
}
