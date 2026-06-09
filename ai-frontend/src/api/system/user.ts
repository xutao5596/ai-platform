import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface UserVO {
  id: number
  username: string
  realName: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  deptId?: number
  deptName?: string
  status: number
  admin: number
  remark?: string
  roleIds?: number[]
  roleCodes?: string[]
  createTime?: string
}

export interface UserSave {
  id?: number
  username: string
  password?: string
  realName?: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  deptId?: number
  status?: number
  remark?: string
  roleIds?: number[]
}

export const userApi = {
  page: (q: PageQuery) => get<PageResult<UserVO>>('/v1/system/user/page', q),
  get: (id: number) => get<UserVO>(`/v1/system/user/${id}`),
  create: (data: UserSave) => post<number>('/v1/system/user', data),
  update: (data: UserSave) => put('/v1/system/user', data),
  remove: (id: number) => del(`/v1/system/user/${id}`),
  removeBatch: (ids: number[]) => del('/v1/system/user/batch', { data: ids }),
  resetPassword: (id: number, newPassword: string) =>
    post(`/v1/system/user/${id}/reset-password`, { newPassword }),
  changePassword: (oldPassword: string, newPassword: string) =>
    post('/v1/system/user/change-password', { oldPassword, newPassword }),
  updateStatus: (id: number, status: number) =>
    post(`/v1/system/user/${id}/status`, { status })
}
