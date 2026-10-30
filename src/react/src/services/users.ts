import { http } from './http'
export interface IManagedUser { id: string; username: string; displayName: string; phone: string | null; email: string | null; enabled: boolean; createdAt: string; version: number }
export interface IUserPage { items: IManagedUser[]; total: number; page: number; size: number }
export const listUsers = async (keyword: string, status: string, page: number, phone = '', email = '') => (await http.post<IUserPage>('/api/users/page', { keyword, phone, email, enabled: status === '' ? undefined : status === 'true', page, size: 10 })).data
export const createUser = async (values: { username: string; displayName: string; phone?: string; email?: string; password: string }) => (await http.post<IManagedUser>('/api/users', values)).data
export const editUser = async (id: string, values: { displayName: string; phone?: string; email?: string; enabled: boolean; version: number }) => (await http.patch<IManagedUser>(`/api/users/${id}`, values)).data
export const userDetail = async (id: string) => (await http.get<IManagedUser>(`/api/users/${id}`)).data
export const deleteUser = async (id: string, version: number) => { await http.delete(`/api/users/${id}`, { params: { version } }) }
export const resetUserPassword = async (id: string, password: string, version: number) => { await http.post(`/api/users/${id}/reset-password`, { password, version }) }
