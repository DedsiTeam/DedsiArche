import { http } from './http'

export type LoginAuditResult = 'SUCCESS' | 'FAILURE'
export type LoginAuditReason = 'LOGIN_SUCCEEDED' | 'BAD_CREDENTIALS' | 'USER_DISABLED' | 'INVALID_REQUEST' | 'SERVICE_ERROR'

export const loginAuditResultOptions = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILURE', label: '失败' },
]
export const loginAuditReasonLabels: Record<LoginAuditReason, string> = {
  LOGIN_SUCCEEDED: '登录成功',
  BAD_CREDENTIALS: '账号或密码错误',
  USER_DISABLED: '账号已停用',
  INVALID_REQUEST: '请求无效',
  SERVICE_ERROR: '服务异常',
}
export const loginAuditReasonOptions = Object.entries(loginAuditReasonLabels).map(([value, label]) => ({ value, label }))

export interface ILoginAudit {
  id: string
  /* 服务端北京时间本地字符串，不经浏览器时区转换。 */
  loginTime: string
  result: LoginAuditResult
  reasonCode: LoginAuditReason
  loginAccount: string | null
  userId: string | null
  displayName: string | null
  clientIp: string | null
  userAgent: string | null
  failureMessage: string | null
  traceId: string | null
}

export interface ILoginAuditPage {
  items: ILoginAudit[]
  total: number
  page: number
  size: number
}

export interface ILoginAuditFilters {
  loginAccount: string
  displayName: string
  clientIp: string
  result: LoginAuditResult | ''
  reasonCode: LoginAuditReason | ''
  from: string
  to: string
  page: number
}

/** 时间筛选为北京时间 [from, to)，空值省略，无 UTC 转换。 */
export async function listLoginAudits(filters: ILoginAuditFilters, signal?: AbortSignal) {
  return (await http.post<ILoginAuditPage>('/api/login-audits/page', {
    loginAccount: filters.loginAccount || undefined,
    displayName: filters.displayName || undefined,
    clientIp: filters.clientIp || undefined,
    result: filters.result || undefined,
    reasonCode: filters.reasonCode || undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    page: filters.page,
    size: 10,
  }, { signal })).data
}

export async function loginAuditDetail(id: string, signal?: AbortSignal) {
  return (await http.get<ILoginAudit>(`/api/login-audits/${encodeURIComponent(id)}`, { signal })).data
}
