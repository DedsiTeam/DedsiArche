import { http } from './http'

export type DictionaryEnabled = '' | 'true' | 'false'
export const dictionaryEnabledOptions = [
  { value: 'true', label: '已启用' },
  { value: 'false', label: '已停用' },
]

/** 字典分页摘要，不包含内部字典项。 */
export interface IDictionarySummary {
  id: string
  code: string
  name: string
  systemCode: string
  systemName: string
  description: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface IDictionaryItem {
  id: string
  dictionaryId: string
  code: string
  name: string
  sortOrder: number
  isDefault: boolean
  enabled: boolean
}

export interface IDictionary extends IDictionarySummary {
  items: IDictionaryItem[]
}

export interface IDictionaryPage {
  items: IDictionarySummary[]
  total: number
  page: number
  size: number
}

export interface IDictionaryFilters {
  keyword: string
  systemCode: string
  status: DictionaryEnabled
  page: number
}

/** 新项不传 id，已有项保留服务端 id 与不可变 code。 */
export interface IDictionaryItemValues {
  id?: string
  code: string
  name: string
  sortOrder: number
  isDefault: boolean
  enabled: boolean
}

export interface IDictionaryValues {
  name: string
  description: string
  enabled: boolean
  items: IDictionaryItemValues[]
}

export async function listDictionaries(filters: IDictionaryFilters, signal?: AbortSignal) {
  return (await http.post<IDictionaryPage>('/api/dictionaries/page', {
    keyword: filters.keyword,
    systemCode: filters.systemCode || undefined,
    enabled: filters.status === '' ? undefined : filters.status === 'true',
    page: filters.page,
    size: 10,
  }, { signal })).data
}

export async function dictionaryDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IDictionary>(`/api/dictionaries/${id}`, { signal })).data
}

export async function createDictionary(values: IDictionaryValues & { systemCode: string; code: string }) {
  return (await http.post<IDictionary>('/api/dictionaries', values)).data
}

export async function editDictionary(id: string, values: IDictionaryValues & { version: number }) {
  return (await http.patch<IDictionary>(`/api/dictionaries/${id}`, values)).data
}

export async function deleteDictionary(id: string, version: number) {
  await http.delete(`/api/dictionaries/${id}`, { params: { version } })
}

/** 登录用户可读取已启用字典及启用项；缺失或停用返回 404。 */
export async function availableDictionary(systemCode: string, code: string, signal?: AbortSignal) {
  return (await http.get<IDictionary>('/api/dictionaries/available', { params: { systemCode, code }, signal })).data
}
