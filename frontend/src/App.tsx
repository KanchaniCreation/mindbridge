import { Navigate, Route, Routes } from 'react-router-dom'
import AuthProvider from './auth/AuthProvider'
import ProtectedRoute from './auth/ProtectedRoute'
import AppLayout from './components/AppLayout'
import DashboardPage from './pages/DashboardPage'
import JournalEditorPage from './pages/JournalEditorPage'
import JournalPage from './pages/JournalPage'
import LoginPage from './pages/LoginPage'
import MoodPage from './pages/MoodPage'
import RegisterPage from './pages/RegisterPage'
import './App.css'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route path="/journal" element={<JournalPage />} />
          <Route path="/journal/new" element={<JournalEditorPage />} />
          <Route path="/journal/:id" element={<JournalEditorPage />} />
          <Route path="/mood" element={<MoodPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  )
}
