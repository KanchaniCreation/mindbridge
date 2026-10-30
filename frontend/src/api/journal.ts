import { apiFetch } from './http'
import type { Page } from './types'

export interface JournalListItem {
  id: string
  title: string | null
  category: string | null
  excerpt: string
  createdAt: string
  updatedAt: string
}

export interface JournalEntry {
  id: string
  title: string | null
  category: string | null
  body: string
  createdAt: string
  updatedAt: string
}

export interface JournalInput {
  title?: string
  category?: string
  body: string
}

export interface JournalQuery {
  q?: string
  category?: string
  page?: number
  size?: number
}

export function listJournal(query: JournalQuery, signal?: AbortSignal): Promise<Page<JournalListItem>> {
  const params = new URLSearchParams()
  if (query.q) params.set('q', query.q)
  if (query.category) params.set('category', query.category)
  params.set('page', String(query.page ?? 0))
  params.set('size', String(query.size ?? 20))
  return apiFetch<Page<JournalListItem>>(`/journal?${params.toString()}`, { signal })
}

export function getJournal(id: string, signal?: AbortSignal): Promise<JournalEntry> {
  return apiFetch<JournalEntry>(`/journal/${id}`, { signal })
}

export function createJournal(input: JournalInput): Promise<JournalEntry> {
  return apiFetch<JournalEntry>('/journal', { method: 'POST', body: input })
}

export function updateJournal(id: string, input: JournalInput): Promise<JournalEntry> {
  return apiFetch<JournalEntry>(`/journal/${id}`, { method: 'PUT', body: input })
}

export function deleteJournal(id: string): Promise<void> {
  return apiFetch<void>(`/journal/${id}`, { method: 'DELETE' })
}

export function getPrompts(signal?: AbortSignal): Promise<string[]> {
  return apiFetch<string[]>('/journal/prompts', { signal })
}
