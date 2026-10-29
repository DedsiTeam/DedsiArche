// Explicit real-server test: creates a uniquely named test user in the configured database.
// Run with: bun run test:e2e:api (start the backend first).
const base = process.env.E2E_API_URL || 'http://localhost:11518/dedsi-arche-api'
const username = `api_e2e_${Date.now()}`
const password = `E2e!${crypto.randomUUID()}`
let checks = 0
async function request(path: string, status: number, method = 'GET', body?: unknown, token?: string) {
  const response = await fetch(`${base}/api/users${path}`, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15_000),
  })
  if (response.status !== status) throw new Error(`${method} ${path}: expected ${status}, received ${response.status}`)
  checks++
  const text = await response.text()
  return text ? JSON.parse(text) : null
}
function check(condition: boolean, message: string) {
  if (!condition) throw new Error(message)
  checks++
}
await request('/me', 401)
await request('/register', 400, 'POST', { username: 'x', displayName: '', password: 'short' })
const user = await request('/register', 201, 'POST', { username, displayName: 'API 端到端测试', password })
check(user.username === username, 'Registration username mismatch')
await request('/register', 409, 'POST', { username, displayName: 'duplicate', password })
await request('/login', 400, 'POST', { username: '', password: '' })
await request('/login', 401, 'POST', { username, password: 'wrong' })
await request('/login', 401, 'POST', { username: 'x'.repeat(200), password: 'x'.repeat(200) })
const first = await request('/login', 200, 'POST', { username, password })
check(!!first.accessToken && !!first.refreshToken, 'Login credentials missing')
const me = await request('/me', 200, 'GET', undefined, first.accessToken)
check(me.id === user.id, 'Authenticated user mismatch')
await request('/me', 400, 'PATCH', { displayName: ' ' }, first.accessToken)
await request('/me', 400, 'PATCH', { displayName: 'x'.repeat(101) }, first.accessToken)
await request('/me', 200, 'PATCH', { displayName: 'API 测试已保存' }, first.accessToken)
const saved = await request('/me', 200, 'GET', undefined, first.accessToken)
check(saved.displayName === 'API 测试已保存', 'Profile did not persist')
const next = await request('/refresh', 200, 'POST', { refreshToken: first.refreshToken })
check(next.refreshToken !== first.refreshToken, 'Refresh token did not rotate')
await request('/refresh', 401, 'POST', { refreshToken: first.refreshToken })
await request('/me', 200, 'GET', undefined, next.accessToken)
await request('/logout', 204, 'POST', { refreshToken: next.refreshToken })
await request('/refresh', 401, 'POST', { refreshToken: next.refreshToken })
const again = await request('/login', 200, 'POST', { username, password })
check(again.user.displayName === 'API 测试已保存', 'Profile lost after re-login')
await request('/logout', 204, 'POST', { refreshToken: again.refreshToken })
console.log(JSON.stringify({ passed: checks, username, userId: user.id, note: 'Test account retained for audit; no password or tokens logged.' }))
