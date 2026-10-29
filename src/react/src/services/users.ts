import { http } from './http'
export interface ManagedUser { id: string; username: string; displayName: string; enabled: boolean; createdAt: string; version: number }
export interface UserPage { items: ManagedUser[]; total: number; page: number; size: number }
export const listUsers = async (keyword: string, status: string, page: number) => (await http.post<UserPage>('/api/users/page', { keyword, enabled: status === '' ? undefined : status === 'true', page, size: 10 })).data
export const createUser = async (values: { username: string; displayName: string; password: string }) => (await http.post<ManagedUser>('/api/users', values)).data
export const editUser = async (id: string, values: { displayName: string; enabled: boolean; version: number }) => (await http.patch<ManagedUser>(`/api/users/${id}`, values)).data
export const userDetail = async (id: string) => (await http.get<ManagedUser>(`/api/users/${id}`)).data
export const deleteUser = async (id: string, version: number) => { await http.delete(`/api/users/${id}`, { params: { version } }) }
export const resetUserPassword = async (id: string, password: string, version: number) => { await http.post(`/api/users/${id}/reset-password`, { password, version }) }
