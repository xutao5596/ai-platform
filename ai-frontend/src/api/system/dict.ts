import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface DictVO {
  id: number
  typeCode: string
  typeName: string
  description?: string
  status: number
  createTime?: string
}

export interface DictItemVO {
  id: number
  typeCode: string
  itemKey: string
  itemValue: string
  label?: string
  color?: string
  sortOrder?: number
  status: number
  remark?: string
}

export const dictApi = {
  page: (q: PageQuery) => get<PageResult<DictVO>>('/v1/system/dict/page', q),
  list: () => get<DictVO[]>('/v1/system/dict/list'),
  items: (typeCode: string) => get<DictItemVO[]>(`/v1/system/dict/items`, { typeCode }),
  createDict: (data: DictVO) => post<number>('/v1/system/dict', data),
  updateDict: (data: DictVO) => put('/v1/system/dict', data),
  removeDict: (id: number) => del(`/v1/system/dict/${id}`),
  createItem: (data: DictItemVO) => post<number>('/v1/system/dict/item', data),
  updateItem: (data: DictItemVO) => put('/v1/system/dict/item', data),
  removeItem: (id: number) => del(`/v1/system/dict/item/${id}`)
}
