import { isAxiosError } from 'axios'
import type { PermissionEnabled } from '@/services/permissions'

export function isPermissionEnabled(value: string): value is PermissionEnabled {
  return value === '' || value === 'true' || value === 'false'
}

export function validatePermissionText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validatePermissionSort(value: string) {
  const number = Number(value)
  return value.trim() && Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? '' : '排序必须为 0～2147483647 的整数。'
}

export function permissionError(cause: unknown, operation: 'load' | 'create' | 'edit' | 'delete' = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  if (status === 409) {
    if (operation === 'delete') {
      // ResponseStatusException 不保证携带 detail；仅匹配已知原因，缺失时保留关联冲突的可能。
      const reason = cause.response?.data?.detail ?? cause.response?.data?.message
      if (reason === '权限已被菜单引用，请先解除菜单权限关联') {
        return '该权限已被菜单引用，请先解除菜单权限关联后再删除。'
      }
      if (reason === '权限已变化，请重新读取后提交'
        || reason === '权限已被修改，请刷新后重试'
        || reason === '数据已变化，请重新读取后提交') {
        return '权限资料已变化，请关闭弹窗、刷新列表后重新删除。'
      }
      return '无法删除：该权限可能仍被菜单或岗位引用，或资料版本已变化。请先解除关联，再刷新资料后重试。'
    }
    return operation === 'create'
      ? '该系统下的权限编码已存在，请更换权限编码。'
      : '权限资料已变化（系统更名也会更新版本），请关闭弹窗、刷新列表后重试。'
  }
  if (status === 404) return '该权限已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理权限，请联系管理员。'
  if (status === 400) return '请检查必填字段、长度和排序，并确认所属系统仍已登记。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '权限服务暂时不可用，请稍后重试。'
}
