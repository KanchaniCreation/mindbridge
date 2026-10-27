import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BackendStatus from './BackendStatus'
import * as client from '../api/client'

vi.mock('../api/client')

const info = { name: 'mindbridge', version: '0.1.0', status: 'UP', serverTime: '2026-10-19T00:00:00Z' }

describe('BackendStatus', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows backend status once loaded', async () => {
    vi.mocked(client.fetchSystemInfo).mockResolvedValue(info)
    render(<BackendStatus />)
    expect(screen.getByRole('status')).toHaveTextContent('Connecting')
    expect(await screen.findByText('Backend status: UP (v0.1.0)')).toBeInTheDocument()
  })

  it('shows an error and recovers on retry', async () => {
    vi.mocked(client.fetchSystemInfo)
      .mockRejectedValueOnce(new Error('Server responded with 500'))
      .mockResolvedValueOnce(info)
    render(<BackendStatus />)
    expect(await screen.findByRole('alert')).toHaveTextContent('500')
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Backend status: UP (v0.1.0)')).toBeInTheDocument()
  })
})
