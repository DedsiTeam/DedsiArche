import { isAxiosError } from 'axios'
import type { IOrganizationNode, OrganizationEnabled } from '@/services/organizations'

export function isOrganizationEnabled(value: string): value is OrganizationEnabled {
  return value === '' || value === 'true' || value === 'false'
}

export function validateOrganizationText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validateOrganizationSort(value: string) {
  const number = Number(value)
  return value.trim() && Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? '' : '排序必须为 0～2147483647 的整数。'
}

export interface IOrganizationParentOption {
  value: string
  label: string
  disabled: boolean
}

/** 排除当前节点及整棵后代子树，层级名称辅助区分同名组织。 */
export function organizationParentOptions(nodes: IOrganizationNode[], currentId: string | undefined, enabled: boolean) {
  const options: IOrganizationParentOption[] = []
  function visit(items: IOrganizationNode[], path: string[]) {
    for (const item of items) {
      if (item.id === currentId) continue
      const names = [...path, item.name]
      options.push({
        value: item.id,
        label: `${names.join(' / ')}（${item.code}）${item.enabled ? '' : ' · 已停用'}`,
        disabled: enabled && !item.enabled,
      })
      visit(item.children, names)
    }
  }
  visit(nodes, [])
  return options
}

export function organizationError(cause: unknown, operation: 'load' | 'create' | 'edit' | 'delete' = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  const code = cause.response?.data?.code
  const detail = cause.response?.data?.detail
  if (code === 'ORGANIZATION_SYSTEM_NOT_FOUND') return '所属系统已不存在，请重新选择。'
  if (status === 409) {
    if (detail === '组织下存在子节点，不能删除') return '此组织仍有下级组织，请先调整或删除全部下级后重试。'
    if (detail === '存在启用的下级组织，不能停用') return '此组织仍有启用的下级，请先停用或调整下级组织。'
    if (detail === '组织已被修改，请刷新后重试' || detail === '数据已变化，请重新读取后提交') return '组织已被修改，请关闭弹窗、刷新后重新编辑。当前输入未自动覆盖。'
  }
  if (status === 409) {
    if (operation === 'create') return '该系统下的组织编码已存在，请更换编码。'
    if (operation === 'delete') return '组织资料已变化或仍有下级组织，不能删除。请刷新并先处理全部下级。'
    return '组织资料已变化，或仍有启用的下级而不能停用。请关闭弹窗、刷新后检查组织状态。'
  }
  if (status === 404) return '该组织已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理组织机构，请联系管理员。'
  if (status === 400) return '请检查字段及上级组织：必须同系统，不能选择自身或后代，启用组织的上级也必须启用。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '组织机构服务暂时不可用，请稍后重试。'
}
