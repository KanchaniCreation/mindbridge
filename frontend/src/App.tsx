import { useEffect, useState } from 'react'
import { fetchSystemInfo, type SystemInfo } from './api/client'
import './App.css'

type State =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; info: SystemInfo }

export default function App() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchSystemInfo(controller.signal)
      .then((info) => setState({ kind: 'ready', info }))
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setState({ kind: 'error', message: e instanceof Error ? e.message : 'Unknown error' })
      })
    return () => controller.abort()
  }, [attempt])

  function retry() {
    setState({ kind: 'loading' })
    setAttempt((n) => n + 1)
  }

  return (
    <main className="page">
      <h1>MindBridge</h1>
      <p>A private space for reflection and self-care.</p>

      {state.kind === 'loading' && <p role="status">Connecting to the server…</p>}

      {state.kind === 'error' && (
        <div role="alert">
          <p>We could not reach the server: {state.message}</p>
          <button onClick={retry}>Retry</button>
        </div>
      )}

      {state.kind === 'ready' && (
        <p role="status">{`Backend status: ${state.info.status} (v${state.info.version})`}</p>
      )}
    </main>
  )
}