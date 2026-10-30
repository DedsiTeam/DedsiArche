import { http } from './http'

/* 人工维护的系统业务状态，与后端 SystemStatus 一致。 */
export type SystemStatus =
  | 'NOT_LAUNCHED'
  | 'NORMAL'
  | 'DISABLED'
  | 'UPDATING'
  | 'MAINTENANCE'
  | 'RETIRED'

export const systemStatusOptions: { value: SystemStatus; label: string }[] = [
  { value: 'NOT_LAUNCHED', label: '未上线' },
  { value: 'NORMAL', label: '正常' },
  { value: 'DISABLED', label: '停用' },
  { value: 'UPDATING', label: '更新中' },
  { value: 'MAINTENANCE', label: '维护中' },
  { value: 'RETIRED', label: '已退役' },
]

export function systemStatusLabel(status: SystemStatus) {
  return systemStatusOptions.find((option) => option.value === status)?.label ?? status
}

export interface IManagedSystem {
  /* 服务端生成的 26 位 ULID，前端仅作字符串传递。 */
  id: string
  code: string
  name: string
  description: string | null
  entryUrl: string | null
  status: SystemStatus
  sortOrder: number
  /* 北京时间本地字符串，展示时不转换时区。 */
  createdAt: string
  updatedAt: string
  version: number
}

export interface IManagedSystemPage {
  items: IManagedSystem[]
  total: number
  page: number
  size: number
}

export interface IManagedSystemValues {
  name: string
  /* 空串清除选填字段；PATCH 的 null/省略则保留原值。 */
  description: string
  entryUrl: string
  status: SystemStatus
  sortOrder: number
}

export interface IManagedSystemFilters {
  keyword: string
  status: SystemStatus | ''
  page: number
}

export async function listManagedSystems(filters: IManagedSystemFilters, signal?: AbortSignal) {
  const { data } = await http.post<IManagedSystemPage>('/api/systems/page', {
    keyword: filters.keyword,
    status: filters.status || undefined,
    page: filters.page,
    size: 10,
  }, { signal })
  return data
}

export async function managedSystemDetail(id: string, signal?: AbortSignal) {
  const { data } = await http.get<IManagedSystem>(`/api/systems/${id}`, { signal })
  return data
}

export interface IManagedSystemOption {
  /* 下拉框提交值。 */
  code: string
  /* 下拉框展示文本。 */
  name: string
}

/** 使用专用轻量接口，不复用分页查询或拉取完整系统资料。 */
export async function listManagedSystemOptions(signal?: AbortSignal) {
  const { data } = await http.get<IManagedSystemOption[]>('/api/systems/options', { signal })
  return data
}

export async function createManagedSystem(values: IManagedSystemValues & { code: string }) {
  const { data } = await http.post<IManagedSystem>('/api/systems', values)
  return data
}

export async function editManagedSystem(id: string, values: IManagedSystemValues & { version: number }) {
  const { data } = await http.patch<IManagedSystem>(`/api/systems/${id}`, values)
  return data
}

export async function deleteManagedSystem(id: string, version: number) {
  await http.delete(`/api/systems/${id}`, { params: { version } })
}
