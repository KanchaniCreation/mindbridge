import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as auth from '../api/auth'
import { ApiError } from '../api/http'
import AuthProvider from '../auth/AuthProvider'
import RegisterPage from './RegisterPage'

vi.mock('../api/auth')

const user = { id: '1', email: 'new@example.com', displayName: null, createdAt: '2026-10-28T00:00:00Z' }

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(auth.getCurrentUser).mockRejectedValue(new ApiError(401, 'Unauthorized'))
  })

  it('rejects a short password without calling the server', async () => {
    renderPage()
    await userEvent.type(screen.getByLabelText('Email'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'short')
    await userEvent.type(screen.getByLabelText('Confirm password'), 'short')
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('at least 12 characters')
    expect(auth.register).not.toHaveBeenCalled()
  })

  it('creates the account and signs the user in', async () => {
    vi.mocked(auth.register).mockResolvedValue(user)
    vi.mocked(auth.login).mockResolvedValue(user)
    renderPage()

    await userEvent.type(screen.getByLabelText('Email'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'correct-horse-battery')
    await userEvent.type(screen.getByLabelText('Confirm password'), 'correct-horse-battery')
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    await vi.waitFor(() => expect(auth.register).toHaveBeenCalledTimes(1))
    expect(auth.register).toHaveBeenCalledWith(
      expect.objectContaining({ email: 'new@example.com', acceptedTerms: true }),
    )
    await vi.waitFor(() => expect(auth.login).toHaveBeenCalledWith('new@example.com', 'correct-horse-battery'))
  })
})
