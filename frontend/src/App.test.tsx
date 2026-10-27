import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as auth from './api/auth'
import * as client from './api/client'
import { ApiError } from './api/http'

vi.mock('./api/auth')
vi.mock('./api/client')

const user = { id: '1', email: 'kanchani@example.com', displayName: 'Kanchani', createdAt: '2026-10-28T00:00:00Z' }
const info = { name: 'mindbridge', version: '0.1.0', status: 'UP', serverTime: '2026-10-28T00:00:00Z' }

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App routing and sign-in', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(client.fetchSystemInfo).mockResolvedValue(info)
  })

  it('sends a signed-out visitor to the sign-in page', async () => {
    vi.mocked(auth.getCurrentUser).mockRejectedValue(new ApiError(401, 'Unauthorized'))
    renderAt('/')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('shows the dashboard when a session already exists', async () => {
    vi.mocked(auth.getCurrentUser).mockResolvedValue(user)
    renderAt('/')
    expect(await screen.findByText('Welcome, Kanchani')).toBeInTheDocument()
  })

  it('shows a clear error for wrong credentials and signs in with the right ones', async () => {
    vi.mocked(auth.getCurrentUser).mockRejectedValue(new ApiError(401, 'Unauthorized'))
    vi.mocked(auth.login)
      .mockRejectedValueOnce(new ApiError(401, 'Invalid email or password.'))
      .mockResolvedValueOnce(user)
    renderAt('/login')

    await userEvent.type(await screen.findByLabelText('Email'), 'kanchani@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-password-123')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Welcome, Kanchani')).toBeInTheDocument()
  })
})
