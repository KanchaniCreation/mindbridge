import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  type RegisterInput,
  type User,
} from '../api/auth'
import { AuthContext, type AuthContextValue } from './authContext'

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  // On first load, ask the server whether our session cookie is still valid.
  useEffect(() => {
    const controller = new AbortController()
    getCurrentUser(controller.signal)
      .then((current) => {
        if (controller.signal.aborted) return
        setUser(current)
        setLoading(false)
      })
      .catch(() => {
        if (controller.signal.aborted) return
        setUser(null)
        setLoading(false)
      })
    return () => controller.abort()
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    setUser(await loginRequest(email, password))
  }, [])

  const register = useCallback(async (input: RegisterInput) => {
    await registerRequest(input)
    setUser(await loginRequest(input.email, input.password))
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } finally {
      setUser(null)
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
