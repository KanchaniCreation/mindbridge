export interface FieldError {
  field: string
  message: string
}

export class ApiError extends Error {
  status: number
  fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.split('=')[1]
}

/** The server puts a CSRF token in a cookie; we echo it in a header on every state-changing call. */
async function ensureCsrfToken(): Promise<string> {
  let token = readCookie('XSRF-TOKEN')
  if (!token) {
    await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
    token = readCookie('XSRF-TOKEN')
  }
  return token ? decodeURIComponent(token) : ''
}

interface RequestOptions {
  method?: string
  body?: unknown
  signal?: AbortSignal
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    headers['X-XSRF-TOKEN'] = await ensureCsrfToken()
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    signal: options.signal,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    let message = 'Something went wrong. Please try again.'
    let fieldErrors: FieldError[] = []
    try {
      const problem = await response.json()
      if (typeof problem.detail === 'string') message = problem.detail
      if (Array.isArray(problem.fieldErrors)) fieldErrors = problem.fieldErrors
    } catch {
      // The body was not JSON; keep the generic message.
    }
    throw new ApiError(response.status, message, fieldErrors)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
