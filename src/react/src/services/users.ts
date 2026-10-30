import { http } from './http'

/** 用户分页摘要，不包含岗位集合。 */
export interface IManagedUser {
  id: string
  username: string
  displayName: string
  phone: string | null
  email: string | null
  enabled: boolean
  createdAt: string
  version: number
}

/** 后端保存的用户岗位关联和配置时快照。 */
export interface IUserPosition {
  id: string
  userId: string
  positionId: string
  positionCode: string
  positionName: string
  systemCode: string | null
  systemName: string | null
}

export interface IManagedUserDetail extends IManagedUser {
  positions: IUserPosition[]
}

export interface IUserPage {
  items: IManagedUser[]
  total: number
  page: number
  size: number
}

interface IUserValues {
  displayName: string
  phone?: string
  email?: string
  // 管理表单整体提交；空数组明确清空，快照不由客户端生成。
  positionIds: string[]
}

export async function listUsers(keyword: string, status: string, page: number, phone = '', email = '') {
  return (await http.post<IUserPage>('/api/users/page', {
    keyword, phone, email, enabled: status === '' ? undefined : status === 'true', page, size: 10,
  })).data
}

export async function createUser(values: IUserValues & { username: string; password: string }) {
  return (await http.post<IManagedUserDetail>('/api/users', values)).data
}

export async function editUser(id: string, values: IUserValues & { enabled: boolean; version: number }) {
  return (await http.patch<IManagedUserDetail>(`/api/users/${id}`, values)).data
}

export async function userDetail(id: string, signal?: AbortSignal) {
  return (await http.get<IManagedUserDetail>(`/api/users/${id}`, { signal })).data
}

export async function deleteUser(id: string, version: number) {
  await http.delete(`/api/users/${id}`, { params: { version } })
}

export async function resetUserPassword(id: string, password: string, version: number) {
  await http.post(`/api/users/${id}/reset-password`, { password, version })
}
