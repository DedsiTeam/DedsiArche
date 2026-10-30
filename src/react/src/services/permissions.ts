import { http } from './http'

export type PermissionEnabled = '' | 'true' | 'false'
export const permissionEnabledOptions = [
  { value: 'true', label: '已启用' },
  { value: 'false', label: '已停用' },
]

export interface IPermission {
  /* 服务端生成的 26 位 ULID。 */
  id: string
  systemCode: string
  /* 系统展示名称由服务端维护，不作为可编辑字段发送。 */
  systemName: string
  code: string
  name: string
  description: string | null
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
  version: number
}

export interface IPermissionPage {
  items: IPermission[]
  total: number
  page: number
  size: number
}

export interface IPermissionFilters {
  keyword: string
  systemCode: string
  status: PermissionEnabled
  page: number
}

export interface IPermissionValues {
  name: string
  description: string
  enabled: boolean
  sortOrder: number
}

export async function listPermissions(filters: IPermissionFilters, signal?: AbortSignal) {
  const { data } = await http.post<IPermissionPage>('/api/permissions/page', {
    keyword: filters.keyword,
    systemCode: filters.systemCode || undefined,
    enabled: filters.status === '' ? undefined : filters.status === 'true',
    page: filters.page,
    size: 10,
  }, { signal })
  return data
}

export async function permissionDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IPermission>(`/api/permissions/${id}`, { signal })).data
}

export async function createPermission(values: IPermissionValues & { systemCode: string; code: string }) {
  return (await http.post<IPermission>('/api/permissions', values)).data
}

/* 后端使用 PUT，提交全部可编辑字段，不套用系统管理的 PATCH。 */
export async function editPermission(id: string, values: IPermissionValues & { version: number }) {
  return (await http.put<IPermission>(`/api/permissions/${id}`, values)).data
}

export async function deletePermission(id: string, version: number) {
  await http.delete(`/api/permissions/${id}`, { params: { version } })
}
