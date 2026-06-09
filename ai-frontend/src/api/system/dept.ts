import { get, post, put, del } from '@/utils/http'

export interface DeptVO {
  id: number
  parentId: number
  name: string
  code?: string
  leader?: string
  phone?: string
  email?: string
  sortOrder?: number
  status: number
}

export const deptApi = {
  tree: () => get<any[]>('/v1/system/dept/tree'),
  list: () => get<DeptVO[]>('/v1/system/dept/list'),
  create: (data: DeptVO) => post<number>('/v1/system/dept', data),
  update: (data: DeptVO) => put('/v1/system/dept', data),
  remove: (id: number) => del(`/v1/system/dept/${id}`)
}
