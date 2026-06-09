import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface ProjectVO {
  id: number
  name: string
  code?: string
  description?: string
  icon?: string
  status: number
  ownerId?: number
  roleCode?: string
  memberCount?: number
  flowCount?: number
  createTime?: string
}

export interface ProjectSave {
  id?: number
  name: string
  code?: string
  description?: string
  icon?: string
  status?: number
}

export interface ProjectMember {
  id: number
  userId: number
  username: string
  realName?: string
  avatar?: string
  roleCode: string
  joinTime?: string
}

export const projectApi = {
  page: (q: PageQuery) => get<PageResult<ProjectVO>>('/v1/project/page', q),
  mine: () => get<any[]>('/v1/project/mine'),
  get: (id: number) => get<ProjectVO>(`/v1/project/${id}`),
  create: (data: ProjectSave) => post<number>('/v1/project', data),
  update: (data: ProjectSave) => put('/v1/project', data),
  remove: (id: number) => del(`/v1/project/${id}`),
  members: (projectId: number) => get<ProjectMember[]>(`/v1/project/${projectId}/members`),
  addMember: (projectId: number, data: any) =>
    post<number>(`/v1/project/member/${projectId}`, data),
  updateMemberRole: (projectId: number, userId: number, roleCode: string) =>
    put(`/v1/project/member/${projectId}/role`, { userId, roleCode }),
  removeMember: (projectId: number, userId: number) =>
    del(`/v1/project/member/${projectId}/${userId}`)
}
