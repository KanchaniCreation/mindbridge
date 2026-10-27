import BackendStatus from '../components/BackendStatus'
import { useAuth } from '../auth/authContext'

export default function DashboardPage() {
  const { user, logout } = useAuth()

  return (
    <main className="page">
      <h1>MindBridge</h1>
      <p>{`Welcome, ${user?.displayName ?? user?.email ?? ''}`}</p>
      <BackendStatus />
      <button onClick={() => void logout()}>Sign out</button>
    </main>
  )
}
