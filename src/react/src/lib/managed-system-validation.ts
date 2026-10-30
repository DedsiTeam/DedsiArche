import { isAxiosError } from 'axios'
import { systemStatusOptions, type SystemStatus } from '@/services/managed-systems'

export function validateSystemText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.trim().length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validateSystemSort(value: string) {
  if (!value.trim()) return '请输入排序值。'
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? ''
    : '排序值必须为 0～2147483647 的整数。'
}

export function isSystemStatus(value: string): value is SystemStatus {
  return systemStatusOptions.some((option) => option.value === value)
}

/* 不使用浏览器的自动补全或 URL 归一化结果替换用户输入。 */
export function validateSystemEntry(value: string, status: string) {
  const entry = value.trim()
  if (!entry) return status === 'NORMAL' ? '正常状态必须填写入口地址。' : ''
  if (entry.length > 2048) return '入口地址最多 2048 个字符。'
  const message = '请输入不含账号密码的完整 HTTP/HTTPS 地址。'
  if (!/^https?:\/\//i.test(entry) || /[\s\\<>"{}|^`]/.test(entry) || /%(?![0-9a-f]{2})/i.test(entry)) {
    return message
  }
  try {
    const url = new URL(entry)
    const authority = entry.match(/^https?:\/\/([^/?#]+)/i)?.[1] ?? ''
    if (!url.hostname || authority.includes('@') || /[^\x00-\x7F]/.test(authority)
      || url.username || url.password || url.port === '0') return message
    return ''
  } catch {
    return message
  }
}

export type SystemOperation = 'load' | 'create' | 'edit' | 'delete'

/* 按操作区分冲突，不沿用 userError 中“所有 409 都是账号重复”的提示。 */
export function managedSystemError(cause: unknown, operation: SystemOperation = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  const code = cause.response?.data?.code
  if (code === 'SYSTEM_ENTRY_REQUIRED') return '正常状态必须填写入口地址。'
  if (code === 'INVALID_SYSTEM_ENTRY_URL') return '入口地址必须为不含账号密码的 HTTP/HTTPS 地址。'
  if (code === 'INVALID_SYSTEM_STATUS') return '请选择有效的系统状态。'
  if (code === 'INVALID_SYSTEM_SORT') return '请填写有效的非负整数排序值。'
  if (status === 409) {
    if (operation === 'create') return '系统编码已存在，请更换编码。'
    if (operation === 'delete') {
      // 仅识别已知业务说明，不向用户透传任意服务器错误文本。
      const detail = cause.response?.data?.detail
      if (detail === '系统下存在权限，不能删除' || detail === '系统下存在岗位，不能删除' || detail === '系统下存在权限或岗位，不能删除') {
        return '该系统仍被权限或岗位引用，不能删除。请先处理关联权限或岗位。'
      }
      if (detail === '系统已被修改，请刷新后重试' || detail === '数据已变化，请重新读取后提交') {
        return '系统资料已变化，请关闭弹窗、刷新列表后重试。'
      }
      return '无法删除：系统资料已变化或仍被权限、岗位引用。请刷新资料并检查关联权限和岗位。'
    }
    return '系统资料已变化，请关闭弹窗、刷新列表后重试。'
  }
  if (status === 404) return '该系统已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理系统，请联系管理员。'
  if (status === 400) return '提交的信息不符合要求，请检查字段格式和长度。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '系统服务暂时不可用，请稍后重试。'
}
