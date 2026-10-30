import { Link } from 'react-router-dom'
import BackendStatus from '../components/BackendStatus'
import { useAuth } from '../auth/authContext'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <main className="content">
      <h1>Home</h1>
      <p>{`Welcome, ${user?.displayName ?? user?.email ?? ''}`}</p>
      <ul className="link-list">
        <li>
          <Link to="/journal/new">Write a private journal entry</Link>
        </li>
        <li>
          <Link to="/mood">Record how you are feeling</Link>
        </li>
      </ul>
      <BackendStatus />
    </main>
  )
}
