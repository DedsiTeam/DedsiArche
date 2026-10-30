import { http } from './http'

export type PositionEnabled = '' | 'true' | 'false'
export const positionEnabledOptions = [
  { value: 'true', label: '已启用' },
  { value: 'false', label: '已停用' },
]

/** 分页摘要；不包含权限集合，编辑必须先读取详情。 */
export interface IPositionSummary {
  id: string
  code: string
  name: string
  /* 旧岗位迁移后可以尚未归属系统；首次编辑必须选择所属系统。 */
  systemCode: string | null
  systemName: string | null
  description: string | null
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
  version: number
}

/** 服务端保存的岗位权限快照。 */
export interface IPositionPermission {
  id: string
  positionId: string
  permissionId: string
  systemCode: string
  systemName: string
  permissionCode: string
  permissionName: string
}

export interface IPosition extends IPositionSummary {
  permissions: IPositionPermission[]
}

export interface IPositionPage {
  items: IPositionSummary[]
  total: number
  page: number
  size: number
}

export interface IPositionFilters {
  keyword: string
  status: PositionEnabled
  page: number
}

/** 前端每次保存整个岗位配置，空数组明确清空权限。 */
export interface IPositionValues {
  name: string
  description: string
  enabled: boolean
  sortOrder: number
  permissionIds: string[]
}

export async function listPositions(filters: IPositionFilters, signal?: AbortSignal) {
  return (await http.post<IPositionPage>('/api/positions/page', {
    keyword: filters.keyword,
    enabled: filters.status === '' ? undefined : filters.status === 'true',
    page: filters.page,
    size: 10,
  }, { signal })).data
}

export async function positionDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IPosition>(`/api/positions/${id}`, { signal })).data
}

export async function createPosition(values: IPositionValues & { code: string; systemCode: string }) {
  return (await http.post<IPosition>('/api/positions', values)).data
}

export async function editPosition(id: string, values: IPositionValues & { version: number; systemCode?: string }) {
  return (await http.patch<IPosition>(`/api/positions/${id}`, values)).data
}

export async function deletePosition(id: string, version: number) {
  await http.delete(`/api/positions/${id}`, { params: { version } })
}
