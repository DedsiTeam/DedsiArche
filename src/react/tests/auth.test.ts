import { expect, test } from 'bun:test'
import { AxiosError } from 'axios'
import { http } from '../src/services/http'

test('login stores session, sends Bearer credentials, and logout revokes before clearing', async () => {
  const storage = new Map<string, string>()
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  } })
  const { login, logout } = await import('../src/services/auth')
  const original = http.defaults.adapter
  const session = { accessToken: 'test-access', refreshToken: 'test-refresh', expiresAt: new Date(Date.now() + 60_000).toISOString(), user: { id: '1', username: 'alice', displayName: 'Alice' } }
  let failLogin = true
  let failLogout = false
  let rejectToken = false
  http.defaults.adapter = async (config) => {
    if (config.url === '/api/users/login') {
      expect(JSON.parse(config.data)).toEqual({ username: 'alice', password: 'test-password' })
      if (failLogin) throw new AxiosError('Invalid credentials')
    } else {
      expect(config.headers.Authorization).toBe('Bearer test-access')
      if (rejectToken) throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
        data: {}, status: 401, statusText: 'Unauthorized', headers: {}, config,
      })
      if (config.url === '/api/users/logout') {
        expect(JSON.parse(config.data)).toEqual({ refreshToken: 'test-refresh' })
        if (failLogout) throw new AxiosError('Unavailable')
      }
    }
    return { data: session, status: 200, statusText: 'OK', headers: {}, config }
  }
  try {
    await expect(login(' alice ', 'test-password')).rejects.toThrow()
    expect(storage.size).toBe(0)
    failLogin = false
    await login(' alice ', 'test-password')
    expect(JSON.parse(storage.get('dedsi-arche.session')!).user.username).toBe('alice')
    expect(storage.get('dedsi-arche.session')).not.toContain('test-password')
    await http.get('/api/users/me')
    failLogout = true
    await expect(logout()).rejects.toThrow()
    expect(storage.size).toBe(1)
    failLogout = false
    await logout()
    expect(storage.size).toBe(0)
    await login('alice', 'test-password')
    rejectToken = true
    await expect(http.get('/api/users/me')).rejects.toThrow()
    expect(storage.size).toBe(0)
  } finally {
    failLogout = false
    await logout()
    http.defaults.adapter = original
  }
})

test('profile updates session and concurrent refreshes share one rotating request', async () => {
  const storage = new Map<string, string>()
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  } })
  const { login, logout, updateProfile, refreshSession } = await import('../src/services/auth')
  const original = http.defaults.adapter
  const user = { id: '1', username: 'alice', displayName: 'Alice' }
  const credentials = { accessToken: 'old-access', refreshToken: 'old-refresh', expiresAt: new Date(Date.now() + 60_000).toISOString(), user }
  let refreshCount = 0
  let failRefresh = false
  http.defaults.adapter = async (config) => {
    let data: unknown = credentials
    if (config.url === '/api/users/me') {
      expect(JSON.parse(config.data)).toEqual({ displayName: 'Updated' })
      data = { ...user, displayName: 'Updated' }
    }
    if (config.url === '/api/users/refresh') {
      refreshCount++
      if (failRefresh) throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, { data: {}, status: 401, statusText: 'Unauthorized', headers: {}, config })
      expect(JSON.parse(config.data)).toEqual({ refreshToken: 'old-refresh' })
      data = { ...credentials, accessToken: 'new-access', refreshToken: 'new-refresh' }
    }
    if (config.url === '/api/users/logout') {
      expect(JSON.parse(config.data)).toEqual({ refreshToken: 'new-refresh' })
      expect(config.headers.Authorization).toBe('Bearer new-access')
    }
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  try {
    await login('alice', 'password')
    await updateProfile(' Updated ')
    expect(JSON.parse(storage.get('dedsi-arche.session')!).user.displayName).toBe('Updated')
    const first = refreshSession()
    const second = refreshSession()
    expect(first).toBe(second)
    await Promise.all([first, second])
    expect(refreshCount).toBe(1)
    expect(JSON.parse(storage.get('dedsi-arche.session')!).refreshToken).toBe('new-refresh')
    await logout()
    expect(storage.size).toBe(0)
    await login('alice', 'password')
    failRefresh = true
    await expect(refreshSession()).rejects.toThrow()
    expect(storage.size).toBe(0)
  } finally { http.defaults.adapter = original }
})
