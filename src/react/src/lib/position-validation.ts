import { isAxiosError } from 'axios'
import type { PositionEnabled } from '@/services/positions'

export function isPositionEnabled(value: string): value is PositionEnabled {
  return value === '' || value === 'true' || value === 'false'
}

export function validatePositionText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validatePositionSort(value: string) {
  const number = Number(value)
  return value.trim() && Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? '' : '排序必须为 0～2147483647 的整数。'
}

export function positionError(cause: unknown, operation: 'load' | 'create' | 'edit' | 'delete' = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  const code = cause.response?.data?.code
  if (status === 409 && code === 'POSITION_IN_USE') return '该岗位仍被用户引用，不能删除。请先解除用户与该岗位的关联后再删除。'
  if (code === 'POSITION_PERMISSION_NOT_FOUND') return '所选权限已不存在，请移除该项或重新选择后保存。'
  if (code === 'DUPLICATE_POSITION_PERMISSION') return '岗位权限不能重复，请检查所选权限。'
  if (status === 409) {
    return operation === 'create'
      ? '岗位编码已存在或所选权限发生变化，请检查后重新提交。'
      : '岗位、所属系统或关联权限已变化，请关闭弹窗、刷新列表后重新编辑，避免覆盖其他修改。'
  }
  if (status === 404) return '该岗位已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理岗位，请联系管理员。'
  if (status === 400) return '请检查岗位必填字段、所属系统、长度、排序及所选权限。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '岗位服务暂时不可用，请稍后重试。'
}
