import { useSyncExternalStore } from 'react'
import { isAxiosError } from 'axios'
import { http } from './http'

export interface UserProfile { id: string; username: string; displayName: string; canManageUsers?: boolean }
interface Session {
  accessToken: string
  refreshToken: string
  expiresAt: string
  user: UserProfile
}

const storageKey = 'dedsi-arche.session'
function readSession(): Session | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(storageKey) || 'null') as Session | null
    return value && typeof value.accessToken === 'string' && typeof value.refreshToken === 'string' &&
      typeof value.user?.username === 'string' && typeof value.user?.displayName === 'string' &&
      Date.parse(value.expiresAt) > Date.now() ? value : null
  } catch { return null }
}
let session = readSession()
const listeners = new Set<() => void>()
let expiryTimer: ReturnType<typeof setTimeout> | undefined

function setSession(value: Session | null) {
  session = value
  try {
    if (value) sessionStorage.setItem(storageKey, JSON.stringify(value))
    else sessionStorage.removeItem(storageKey)
  } catch { /* Storage may be disabled; keep this session in memory. */ }
  clearTimeout(expiryTimer)
  if (value) expiryTimer = setTimeout(() => setSession(null), Math.max(0, Date.parse(value.expiresAt) - Date.now()))
  listeners.forEach((listener) => listener())
}
setSession(session)

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
export function useSession() {
  return useSyncExternalStore(subscribe, () => session)
}

export function clearLocalSession() { setSession(null) }

http.interceptors.request.use((config) => {
  if (session && Date.parse(session.expiresAt) <= Date.now()) setSession(null)
  if (session && !config.url?.endsWith('/login')) config.headers.Authorization = `Bearer ${session.accessToken}`
  return config
})

http.interceptors.response.use((response) => response, (error: unknown) => {
  // Ignore failed login attempts and responses from a previous session.
  if (isAxiosError(error) && error.response?.status === 401 && session &&
    error.config?.headers.Authorization === `Bearer ${session.accessToken}`) {
    setSession(null)
  }
  return Promise.reject(error)
})

export async function login(username: string, password: string) {
  const { data } = await http.post<Session>('/api/users/login', { username: username.trim(), password })
  if (!data.accessToken || !data.refreshToken || !data.user?.username || typeof data.user.displayName !== 'string' || !(Date.parse(data.expiresAt) > Date.now())) {
    throw new Error('登录响应无效，请稍后重试。')
  }
  setSession(data)
}

export async function logout() {
  if (refreshing) await refreshing.catch(() => undefined)
  const refreshToken = session?.refreshToken
  if (refreshToken) await http.post('/api/users/logout', { refreshToken })
  setSession(null)
}

let refreshing: Promise<void> | undefined
export function refreshSession(): Promise<void> {
  if (refreshing) return refreshing
  const current = session
  if (!current) return Promise.reject(new Error('请先登录。'))
  refreshing = http.post<Session>('/api/users/refresh', { refreshToken: current.refreshToken })
    .then(({ data }) => {
      if (!data.accessToken || !data.refreshToken || !data.user?.id || !(Date.parse(data.expiresAt) > Date.now())) throw new Error('登录凭据刷新失败。')
      if (session?.refreshToken === current.refreshToken) setSession(data)
    }).finally(() => { refreshing = undefined })
  return refreshing
}

async function profileRequest(displayName?: string) {
  const current = session
  const { data } = displayName === undefined
    ? await http.get<UserProfile>('/api/users/me')
    : await http.patch<UserProfile>('/api/users/me', { displayName: displayName.trim() })
  if (session && session.accessToken === current?.accessToken) setSession({ ...session, user: data })
  return data
}
export const getProfile = () => profileRequest()
export const updateProfile = (displayName: string) => profileRequest(displayName)
