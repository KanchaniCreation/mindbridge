import { apiFetch } from './http'
import type { Page } from './types'

export interface MoodEntry {
  id: string
  moodValue: number
  emotions: string[]
  tags: string[]
  note: string | null
  recordedAt: string
}

export interface MoodInput {
  moodValue: number
  emotions: string[]
  tags: string[]
  note?: string
}

export interface NamedCount {
  name: string
  count: number
}

export interface DailyMood {
  date: string
  averageMood: number
  entries: number
}

export type MoodPeriod = 'WEEK' | 'MONTH'

export interface MoodSummary {
  period: MoodPeriod
  from: string
  to: string
  timezone: string
  entryCount: number
  daysWithCheckIn: number
  minimumEntries: number
  sufficientData: boolean
  moodCounts: Record<string, number>
  topEmotions: NamedCount[]
  topTags: NamedCount[]
  averageMood: number | null
  firstHalfAverage: number | null
  secondHalfAverage: number | null
  direction: 'HIGHER' | 'LOWER' | 'SIMILAR' | null
  daily: DailyMood[]
  note: string
}

export function createMood(input: MoodInput): Promise<MoodEntry> {
  return apiFetch<MoodEntry>('/moods', { method: 'POST', body: input })
}

export function listMoods(
  query: { page?: number; size?: number },
  signal?: AbortSignal,
): Promise<Page<MoodEntry>> {
  const params = new URLSearchParams()
  params.set('page', String(query.page ?? 0))
  params.set('size', String(query.size ?? 10))
  return apiFetch<Page<MoodEntry>>(`/moods?${params.toString()}`, { signal })
}

export function getMoodSummary(
  period: MoodPeriod,
  timezone: string,
  signal?: AbortSignal,
): Promise<MoodSummary> {
  const params = new URLSearchParams({ period, timezone })
  return apiFetch<MoodSummary>(`/moods/summary?${params.toString()}`, { signal })
}

export function deleteMood(id: string): Promise<void> {
  return apiFetch<void>(`/moods/${id}`, { method: 'DELETE' })
}
