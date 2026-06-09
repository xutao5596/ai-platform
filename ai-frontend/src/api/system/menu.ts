import { get, post, put, del } from '@/utils/http'

export interface MenuNode {
  id: number
  parentId: number
  name: string
  title: string
  path?: string
  component?: string
  icon?: string
  permCode?: string
  type: number
  sortOrder?: number
  visible: number
  status: number
  redirect?: string
  remark?: string
  children?: MenuNode[]
}

export interface MenuSave {
  id?: number
  parentId?: number
  name: string
  title: string
  path?: string
  component?: string
  icon?: string
  permCode?: string
  type?: number
  sortOrder?: number
  visible?: number
  status?: number
  redirect?: string
  remark?: string
}

export const menuApi = {
  tree: () => get<MenuNode[]>('/v1/system/menu/tree'),
  create: (data: MenuSave) => post<number>('/v1/system/menu', data),
  update: (data: MenuSave) => put('/v1/system/menu', data),
  remove: (id: number) => del(`/v1/system/menu/${id}`)
}
