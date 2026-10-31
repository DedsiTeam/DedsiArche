import { http } from './http'

export type OrganizationEnabled = '' | 'true' | 'false'
export const organizationEnabledOptions = [
  { value: 'true', label: '已启用' },
  { value: 'false', label: '已停用' },
]

export interface IOrganization {
  id: string
  systemCode: string
  systemName: string
  /* null 表示根组织。 */
  parentId: string | null
  code: string
  name: string
  name1: string | null
  name2: string | null
  name3: string | null
  name4: string | null
  sortOrder: number
  description: string | null
  enabled: boolean
  version: number
  createdAt: string
  updatedAt: string
}

/** 服务端权威组织树，包含停用节点。 */
export interface IOrganizationNode extends IOrganization {
  children: IOrganizationNode[]
}

export interface IOrganizationPage {
  items: IOrganization[]
  total: number
  page: number
  size: number
}

export interface IOrganizationFilters {
  keyword: string
  systemCode: string
  status: OrganizationEnabled
  page: number
}

export interface IOrganizationValues {
  /* 编辑必须显式提交，null 将组织移到根级。 */
  parentId: string | null
  name: string
  name1: string
  name2: string
  name3: string
  name4: string
  sortOrder: number
  description: string
  enabled: boolean
}

export async function listOrganizations(filters: IOrganizationFilters, signal?: AbortSignal) {
  return (await http.post<IOrganizationPage>('/api/organizations/page', {
    keyword: filters.keyword,
    systemCode: filters.systemCode || undefined,
    enabled: filters.status === '' ? undefined : filters.status === 'true',
    page: filters.page,
    size: 10,
  }, { signal })).data
}

export async function organizationTree(systemCode: string, signal?: AbortSignal) {
  return (await http.get<IOrganizationNode[]>(`/api/organizations/tree/${encodeURIComponent(systemCode)}`, { signal })).data
}

export async function organizationDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IOrganization>(`/api/organizations/${id}`, { signal })).data
}

export async function createOrganization(values: IOrganizationValues & { systemCode: string; code: string }) {
  return (await http.post<IOrganization>('/api/organizations', values)).data
}

export async function editOrganization(id: string, values: IOrganizationValues & { version: number }) {
  return (await http.patch<IOrganization>(`/api/organizations/${id}`, values)).data
}

export async function deleteOrganization(id: string, version: number) {
  await http.delete(`/api/organizations/${id}`, { params: { version } })
}
