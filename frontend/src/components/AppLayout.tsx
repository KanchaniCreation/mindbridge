import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/authContext'

export default function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand">MindBridge</span>
        <nav aria-label="Main">
          <NavLink to="/" end>
            Home
          </NavLink>
          <NavLink to="/journal">Journal</NavLink>
          <NavLink to="/mood">Mood</NavLink>
        </nav>
        <div className="header-user">
          <span>{user?.displayName ?? user?.email}</span>
          <button onClick={() => void logout()}>Sign out</button>
        </div>
      </header>
      <Outlet />
    </div>
  )
}
