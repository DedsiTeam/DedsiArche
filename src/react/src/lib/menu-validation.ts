import { isAxiosError } from 'axios'
import type { IMenuNode, MenuEnabled } from '@/services/menus'

export function isMenuEnabled(value: string): value is MenuEnabled {
  return value === '' || value === 'true' || value === 'false'
}

export function validateMenuText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validateMenuSort(value: string) {
  const number = Number(value)
  return value.trim() && Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? '' : '排序必须为 0～2147483647 的整数。'
}

export interface IMenuParentOption {
  value: string
  label: string
  disabled: boolean
}

/** 排除当前节点及整棵后代子树，层级名称辅助区分同名菜单。 */
export function menuParentOptions(nodes: IMenuNode[], currentId: string | undefined, enabled: boolean) {
  const options: IMenuParentOption[] = []
  function visit(items: IMenuNode[], path: string[]) {
    for (const item of items) {
      if (item.id === currentId) continue
      const names = [...path, item.name]
      if (item.menuType === 'DIRECTORY') options.push({
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

export function menuError(cause: unknown, operation: 'load' | 'create' | 'edit' | 'delete' = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  const code = cause.response?.data?.code
  const detail = cause.response?.data?.detail
  if (status === 400 && code === 'INVALID_MENU') {
    const message = cause.response?.data?.message
    if (message === '菜单权限必须属于同一系统' || message === '所选权限不存在') return '绑定权限不存在或不属于当前系统，请重新选择权限。'
  }
  if (code === 'MENU_SYSTEM_NOT_FOUND') return '所属系统已不存在，请重新选择。'
  if (status === 409) {
    if (detail === '该系统下菜单编码已存在') return '该系统下的菜单编码已存在，请更换编码。'
    if (detail === '存在下级菜单，不能改为非目录类型') return '此目录仍有下级菜单，请先调整全部下级，再更改菜单类型。'
    if (detail === '菜单下存在子节点，不能删除') return '此菜单仍有下级菜单，请先调整或删除全部下级后重试。'
    if (detail === '存在启用的下级菜单，不能停用') return '此菜单仍有启用的下级，请先停用或调整下级菜单。'
    if (detail === '菜单已被修改，请刷新后重试' || detail === '数据已变化，请重新读取后提交') return '菜单已被修改，请关闭弹窗、刷新后重新编辑。当前输入未自动覆盖。'
  }
  if (status === 409) {
    if (detail === '该系统下页面路径已存在') return '该系统下的页面路由已存在，请更换路由。'
    if (operation === 'create') return '该系统下的菜单编码或页面路由已存在，请检查并调整。'
    if (operation === 'delete') return '菜单资料已变化或仍有下级菜单，不能删除。请刷新并先处理全部下级。'
    return '菜单资料已变化，或仍有启用的下级而不能停用。请关闭弹窗、刷新后检查菜单状态。'
  }
  if (status === 404) return '该菜单已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理菜单，请联系管理员。'
  if (status === 400) return '请检查菜单类型、路由、组件路径和权限；上级必须是同系统目录，不能选择自身或后代，启用菜单的上级也必须启用。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '菜单服务暂时不可用，请稍后重试。'
}

export function validateMenuRoute(value: string, menuType: string) {
  if (menuType === 'DIRECTORY') return ''
  if (!value.trim()) return menuType === 'EXTERNAL' ? '请输入外链地址。' : '请输入页面路由。'
  const maxLength = menuType === 'PAGE' ? 500 : 2048
  if (value.length > maxLength) return `地址最多 ${maxLength} 个字符。`
  if (menuType === 'PAGE' && (!value.trim().startsWith('/') || value.trim().startsWith('//') || /[?#:\\]/.test(value))) return '页面路由必须以单个 / 开头，不含协议、查询参数、片段或反斜杠。'
  if (menuType === 'PAGE') {
    try {
      const decoded = decodeURIComponent(value.trim())
      if (decoded.split('/').some((part) => part === '.' || part === '..') || /[\\\u0000-\u001f]/.test(decoded)) return '页面路由不能包含路径穿越、反斜杠或控制字符。'
    } catch { return '页面路由包含无效的地址编码。' }
  }
  if (menuType === 'EXTERNAL') {
    try {
      const url = new URL(value.trim())
      if (!['http:', 'https:'].includes(url.protocol) || !url.hostname || url.username || url.password) return '请输入不含账号密码的完整 HTTP/HTTPS 地址。'
    } catch { return '请输入完整 HTTP/HTTPS 地址。' }
  }
  return ''
}

/** 层级只从完整树派生，不作为可编辑字段或保存快照。 */
export function menuLevel(nodes: IMenuNode[], id: string, level = 1): number | undefined {
  for (const node of nodes) {
    if (node.id === id) return level
    const found = menuLevel(node.children, id, level + 1)
    if (found !== undefined) return found
  }
  return undefined
}

export function validateMenuComponent(value: string) {
  const path = value.trim()
  if (!path) return '请输入组件路径。'
  if (path.length > 255) return '组件路径最多 255 个字符。'
  if (!/^[A-Za-z0-9_./-]+$/.test(path) || path.split('/').some((part) => !part || part === '.' || part === '..')) {
    return '使用字母、数字、下划线、点、短横线及 / 分隔的相对逻辑路径，不允许空段、. 或 ..。'
  }
  return ''
}
