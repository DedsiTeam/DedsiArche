// Run explicitly against a local backend. Creates an isolated managed test account.
const base = process.env.E2E_API_URL || 'http://localhost:11518/dedsi-arche-api'
const adminName = process.env.E2E_ADMIN_USERNAME
const adminPassword = process.env.E2E_ADMIN_PASSWORD
if (!adminName || !adminPassword) throw new Error('Set E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD')
let checks = 0
async function request(path: string, status: number, method = 'GET', body?: unknown, token?: string) {
  const response = await fetch(`${base}/api/users${path}`, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) })
  if (response.status !== status) throw new Error(`${method} ${path}: expected ${status}, received ${response.status}`)
  checks++
  const text = await response.text()
  return text ? JSON.parse(text) : null
}
function check(condition: boolean, message: string) { if (!condition) throw new Error(message); checks++ }
const admin = await request('/login', 200, 'POST', { username: adminName, password: adminPassword })
const token = admin.accessToken
check(admin.user.canManageUsers === true, 'Administrator flag missing')
const username = `manage_e2e_${Date.now()}`
const password = `E2e!${crypto.randomUUID()}`
try {
  await request('/page', 401, 'POST', {})
  const created = await request('', 201, 'POST', { username, password, displayName: '管理端测试' }, token)
  const normal = await request('/login', 200, 'POST', { username, password })
  await request('/page', 403, 'POST', {}, normal.accessToken)
  await request(`/${created.id}`, 403, 'PATCH', { displayName: 'Denied', enabled: false, version: 0 }, normal.accessToken)
  await request('', 409, 'POST', { username, password, displayName: 'Duplicate' }, token)
  const results = await request('/page', 200, 'POST', { keyword: username, page: 1, size: 1 }, token)
  check(results.total === 1 && results.items[0].id === created.id, 'Search mismatch')
  check(!JSON.stringify(results).includes('password'), 'Credential field leaked')
  await request('/page', 400, 'POST', { page: 0 }, token)
  const self = await request('/page', 200, 'POST', { keyword: adminName }, token)
  const current = self.items.find((item: { id: string }) => item.id === admin.user.id)
  await request(`/${current.id}`, 400, 'PATCH', { displayName: current.displayName, enabled: false, version: current.version }, token)
  const disabled = await request(`/${created.id}`, 200, 'PATCH', { displayName: '管理端测试已编辑', enabled: false, version: created.version }, token)
  await request('/me', 401, 'GET', undefined, normal.accessToken)
  await request('/login', 400, 'POST', { username, password })
  await request('/refresh', 401, 'POST', { refreshToken: normal.refreshToken })
  const filtered = await request('/page', 200, 'POST', { keyword: username, enabled: false }, token)
  check(filtered.total === 1 && filtered.items[0].displayName === '管理端测试已编辑', 'Disabled filter/persistence mismatch')
  await request(`/${created.id}`, 409, 'PATCH', { displayName: 'Stale', enabled: true, version: created.version }, token)
  await request(`/${created.id}`, 200, 'PATCH', { displayName: '管理端测试已编辑', enabled: true, version: disabled.version }, token)
  const relogin = await request('/login', 200, 'POST', { username, password })
  await request('/logout', 204, 'POST', { refreshToken: relogin.refreshToken })
  const detail = await request(`/${created.id}`, 200, 'GET', undefined, token)
  check(detail.username === username && detail.version === 2, 'Detail mismatch')
  await request(`/${created.id}`, 403, 'GET', undefined, relogin.accessToken)
  await request(`/${created.id}/reset-password`, 403, 'POST', { password: 'Reset#2026!Password', version: 2 }, relogin.accessToken)
  await request(`/${created.id}?version=2`, 403, 'DELETE', undefined, relogin.accessToken)
  await request(`/${admin.user.id}?version=${current.version}`, 400, 'DELETE', undefined, token)
  await request(`/${created.id}/reset-password`, 400, 'POST', { password: 'short', version: 2 }, token)
  await request(`/${created.id}/reset-password`, 409, 'POST', { password: 'Reset#2026!Password', version: 0 }, token)
  const beforeReset = await request('/login', 200, 'POST', { username, password })
  await request(`/${created.id}/reset-password`, 204, 'POST', { password: 'Reset#2026!Password', version: 2 }, token)
  await request('/login', 401, 'POST', { username, password })
  await request('/me', 401, 'GET', undefined, beforeReset.accessToken)
  await request('/refresh', 401, 'POST', { refreshToken: beforeReset.refreshToken })
  const afterReset = await request('/login', 200, 'POST', { username, password: 'Reset#2026!Password' })
  await request('/me', 200, 'GET', undefined, afterReset.accessToken)
  await request(`/${created.id}?version=2`, 409, 'DELETE', undefined, token)
  await request(`/${created.id}?version=3`, 204, 'DELETE', undefined, token)
  await request(`/${created.id}`, 404, 'GET', undefined, token)
  await request('/me', 401, 'GET', undefined, afterReset.accessToken)
  await request('/refresh', 401, 'POST', { refreshToken: afterReset.refreshToken })
  await request('/login', 401, 'POST', { username, password: 'Reset#2026!Password' })
  const absent = await request('/page', 200, 'POST', { keyword: username }, token)
  check(absent.total === 0, 'Deleted user still present')
  console.log(JSON.stringify({ passed: checks, username, userId: created.id, note: 'Created test user deleted; original users unchanged.' }))
} finally { await request('/logout', 204, 'POST', { refreshToken: admin.refreshToken }) }
