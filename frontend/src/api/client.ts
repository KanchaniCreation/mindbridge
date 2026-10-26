export interface SystemInfo {
  name: string
  version: string
  status: string
  serverTime: string
}

export async function fetchSystemInfo(signal?: AbortSignal): Promise<SystemInfo> {
  const res = await fetch('/api/v1/system/info', {
    signal,
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) {
    throw new Error(`Server responded with ${res.status}`)
  }
  return res.json()
}
