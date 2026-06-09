import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface RoleVO {
  id: number
  name: string
  code: string
  description?: string
  status: number
  dataScope?: number
  sortOrder?: number
  menuIds?: number[]
  permissions?: string[]
  createTime?: string
}

export interface RoleSave {
  id?: number
  name: string
  code: string
  description?: string
  status?: number
  dataScope?: number
  sortOrder?: number
  menuIds?: number[]
  permissions?: string[]
}

export const roleApi = {
  page: (q: PageQuery) => get<PageResult<RoleVO>>('/v1/system/role/page', q),
  list: () => get<RoleVO[]>('/v1/system/role/list'),
  get: (id: number) => get<RoleVO>(`/v1/system/role/${id}`),
  create: (data: RoleSave) => post<number>('/v1/system/role', data),
  update: (data: RoleSave) => put('/v1/system/role', data),
  remove: (id: number) => del(`/v1/system/role/${id}`)
}
