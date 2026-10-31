import { http } from './http'

export type MenuType = 'DIRECTORY' | 'PAGE' | 'EXTERNAL'
export const menuTypeOptions = [
  { value: 'DIRECTORY', label: '目录' },
  { value: 'PAGE', label: '页面' },
  { value: 'EXTERNAL', label: '外链' },
]
export const menuTypeLabels: Record<MenuType, string> = { DIRECTORY: '目录', PAGE: '页面', EXTERNAL: '外链' }

export type MenuEnabled = '' | 'true' | 'false'
export const menuEnabledOptions = [
  { value: 'true', label: '已启用' },
  { value: 'false', label: '已停用' },
]

export interface IMenu {
  id: string
  systemCode: string
  systemName: string
  /* null 表示根菜单。 */
  parentId: string | null
  code: string
  name: string
  menuType: MenuType
  permissionId: string | null
  permissionCode: string | null
  permissionName: string | null
  routePath: string | null
  componentPath: string | null
  visible: boolean
  cache: boolean
  affix: boolean
  sortOrder: number
  description: string | null
  enabled: boolean
  version: number
  createdAt: string
  updatedAt: string
}

/** 服务端权威菜单树，包含停用节点。 */
export interface IMenuNode extends IMenu {
  children: IMenuNode[]
}

export interface IMenuPage {
  items: IMenu[]
  total: number
  page: number
  size: number
}

export interface IMenuFilters {
  keyword: string
  systemCode: string
  status: MenuEnabled
  menuType: MenuType | ''
  page: number
}

export interface IMenuValues {
  /* 编辑必须显式提交，null 将菜单移到根级。 */
  parentId: string | null
  name: string
  menuType: MenuType
  permissionId: string | null
  routePath: string
  componentPath: string
  visible: boolean
  cache: boolean
  affix: boolean
  sortOrder: number
  description: string
  enabled: boolean
}

export async function listMenus(filters: IMenuFilters, signal?: AbortSignal) {
  return (await http.post<IMenuPage>('/api/menus/page', {
    keyword: filters.keyword,
    menuType: filters.menuType || undefined,
    systemCode: filters.systemCode || undefined,
    enabled: filters.status === '' ? undefined : filters.status === 'true',
    page: filters.page,
    size: 10,
  }, { signal })).data
}

export async function menuTree(systemCode: string, signal?: AbortSignal) {
  return (await http.get<IMenuNode[]>(`/api/menus/tree/${encodeURIComponent(systemCode)}`, { signal })).data
}

export async function menuDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IMenu>(`/api/menus/${id}`, { signal })).data
}

export async function createMenu(values: IMenuValues & { systemCode: string; code: string }) {
  return (await http.post<IMenu>('/api/menus', values)).data
}

export async function editMenu(id: string, values: IMenuValues & { version: number }) {
  return (await http.patch<IMenu>(`/api/menus/${id}`, values)).data
}

export async function deleteMenu(id: string, version: number) {
  await http.delete(`/api/menus/${id}`, { params: { version } })
}
