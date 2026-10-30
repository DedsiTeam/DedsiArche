import { useSyncExternalStore } from 'react'
import { isAxiosError } from 'axios'
import { http } from './http'

export interface IUserProfile { id: string; username: string; displayName: string; canManageUsers?: boolean }
interface ISession {
  accessToken: string
  refreshToken: string
  expiresAt: string
  expiresAtEpochMs: number
  user: IUserProfile
}

/** 只接受北京时间本地字符串；认证计时独立使用协议数值，不解析或转换展示值。 */
function hasValidExpiry(value: ISession): boolean {
  return typeof value.expiresAt === 'string' &&
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?$/.test(value.expiresAt) &&
    Number.isFinite(value.expiresAtEpochMs)
}

const storageKey = 'dedsi-arche.session'
function readSession(): ISession | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(storageKey) || 'null') as ISession | null
    return value && typeof value.accessToken === 'string' && typeof value.refreshToken === 'string' &&
      hasValidExpiry(value) &&
      typeof value.user?.id === 'string' && Boolean(value.user.id) &&
      typeof value.user.username === 'string' && typeof value.user.displayName === 'string' &&
      value.expiresAtEpochMs > Date.now() ? value : null
  } catch { return null }
}
let session = readSession()
/* 同一会话的资料请求共享 Promise，避免 StrictMode 和并发页面重复请求。 */
let profileRevision = 0
let profileFlight: { token: string; revision: number; promise: Promise<IUserProfile> } | undefined
const listeners = new Set<() => void>()
let expiryTimer: ReturnType<typeof setTimeout> | undefined

function setSession(value: ISession | null) {
  if (session?.accessToken !== value?.accessToken || !value) {
    profileRevision += 1
    profileFlight = undefined
  }
  session = value
  try {
    if (value) sessionStorage.setItem(storageKey, JSON.stringify(value))
    else sessionStorage.removeItem(storageKey)
  } catch { /* Storage may be disabled; keep this session in memory. */ }
  clearTimeout(expiryTimer)
  if (value) expiryTimer = setTimeout(() => setSession(null), Math.max(0, value.expiresAtEpochMs - Date.now()))
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
  if (session && session.expiresAtEpochMs <= Date.now()) setSession(null)
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
  const { data } = await http.post<ISession>('/api/users/login', { username: username.trim(), password })
  if (!hasValidExpiry(data) || !data.accessToken || !data.refreshToken || !data.user?.id || !data.user.username || typeof data.user.displayName !== 'string' || !(data.expiresAtEpochMs > Date.now())) {
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
  refreshing = http.post<ISession>('/api/users/refresh', { refreshToken: current.refreshToken })
    .then(({ data }) => {
      if (!hasValidExpiry(data) || !data.accessToken || !data.refreshToken || !data.user?.id || !(data.expiresAtEpochMs > Date.now())) throw new Error('登录凭据刷新失败。')
      if (session?.refreshToken === current.refreshToken) setSession(data)
    }).finally(() => { refreshing = undefined })
  return refreshing
}

/** 登录、刷新令牌和 /me 返回的完整资料均可复用，包含 false 的普通用户能力值。 */
export function hasUserProfile(value: IUserProfile | undefined): boolean {
  return Boolean(value && typeof value.id === 'string' && value.id &&
    typeof value.username === 'string' && typeof value.displayName === 'string' &&
    typeof value.canManageUsers === 'boolean')
}

/** 优先读取 sessionStorage 恢复的会话资料；缺少完整资料才请求 /me。 */
export function getProfile(force = false): Promise<IUserProfile> {
  const current = session
  if (!current || current.expiresAtEpochMs <= Date.now()) {
    setSession(null)
    return Promise.reject(new Error('请先登录。'))
  }
  if (profileFlight?.token === current.accessToken && profileFlight.revision === profileRevision) {
    return profileFlight.promise
  }
  if (!force && hasUserProfile(current.user)) return Promise.resolve(current.user)

  const revision = profileRevision
  const promise = http.get<IUserProfile>('/api/users/me')
    .then(({ data }) => {
      if (!hasUserProfile(data) || data.id !== current.user.id) throw new Error('用户资料响应无效。')
      if (!session || session.accessToken !== current.accessToken || profileRevision !== revision) {
        throw new Error('会话或用户资料已变化，请重新读取。')
      }
      // setSession 同时更新内存订阅和 sessionStorage 中的 user。
      setSession({ ...session, user: data })
      return data
    })
    .finally(() => {
      if (profileFlight?.promise === promise) profileFlight = undefined
    })
  profileFlight = { token: current.accessToken, revision, promise }
  return promise
}

/** 管理员修改当前账户后主动刷新，禁止之前的读请求覆盖最新资料。 */
export function refreshProfile(): Promise<IUserProfile> {
  profileRevision += 1
  profileFlight = undefined
  return getProfile(true)
}

export async function updateProfile(displayName: string) {
  const current = session
  if (!current) throw new Error('请先登录。')
  const { data } = await http.patch<IUserProfile>('/api/users/me', { displayName: displayName.trim() })
  if (!hasUserProfile(data) || data.id !== current.user.id) throw new Error('用户资料响应无效。')
  if (session && session.accessToken === current.accessToken) {
    profileRevision += 1
    profileFlight = undefined
    setSession({ ...session, user: data })
  }
  return data
}
