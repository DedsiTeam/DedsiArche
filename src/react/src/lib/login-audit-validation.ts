import { isAxiosError } from 'axios'
import { loginAuditReasonLabels, type LoginAuditReason, type LoginAuditResult } from '@/services/login-audits'

export function isLoginAuditResult(value: string): value is LoginAuditResult | '' {
  return value === '' || value === 'SUCCESS' || value === 'FAILURE'
}

export function isLoginAuditReason(value: string): value is LoginAuditReason | '' {
  return value === '' || Object.prototype.hasOwnProperty.call(loginAuditReasonLabels, value)
}

/** 仅比较同格式北京时间文本；不使用 Date 隐式转换时区。 */
export function normalizeLoginAuditTime(value: string) {
  return value.length === 16 ? `${value}:00` : value
}

export function validateLoginAuditTime(value: string) {
  if (!value) return ''
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value)
  if (!match) return '请输入完整的北京时间，精确到分钟或秒。'
  const [year, month, day, hour, minute] = match.slice(1, 6).map(Number)
  const second = match[6] ? Number(match[6]) : 0
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  if (year < 1 || month < 1 || month > 12 || day < 1 || day > days[month - 1]
    || hour > 23 || minute > 59 || Number(second) > 59) return '请输入有效的北京时间。'
  return ''
}

export function loginAuditError(cause: unknown) {
  if (!isAxiosError(cause)) return '加载登录审计失败，请稍后重试。'
  const status = cause.response?.status
  if (status === 400) return '筛选条件无效，请检查字段长度和时间范围；结束时间须晚于开始时间。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权查看登录审计，请联系管理员。'
  if (status === 404) return '此登录审计记录不存在，请关闭弹窗并刷新列表。'
  if (status === 429) return '查询过于频繁，请稍后重试。'
  return '登录审计服务暂时不可用，请稍后重试。'
}
