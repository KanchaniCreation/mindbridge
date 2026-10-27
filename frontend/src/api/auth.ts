import { apiFetch } from './http'

export interface User {
  id: string
  email: string
  displayName: string | null
  createdAt: string
}

export interface RegisterInput {
  email: string
  password: string
  displayName?: string
  acceptedTerms: boolean
}

export function register(input: RegisterInput): Promise<User> {
  return apiFetch<User>('/auth/register', { method: 'POST', body: input })
}

export function login(email: string, password: string): Promise<User> {
  return apiFetch<User>('/auth/login', { method: 'POST', body: { email, password } })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/auth/logout', { method: 'POST' })
}

export function getCurrentUser(signal?: AbortSignal): Promise<User> {
  return apiFetch<User>('/users/me', { signal })
}
