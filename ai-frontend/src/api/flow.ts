import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'
import type {
  FlowVO,
  FlowSave,
  FlowVersionVO,
  FlowVersionSave,
  FlowRunVO,
  FlowRunStepVO,
  FlowTriggerVO,
  FlowTriggerSave,
  NodeDefinition,
  CustomNodeVO,
  CustomNodeSave
} from '@/types/flow'

export const flowApi = {
  page: (q: PageQuery) => get<PageResult<FlowVO>>('/v1/flow/page', q),
  list: (projectId: number) => get<FlowVO[]>('/v1/flow/list', { projectId }),
  get: (id: number) => get<FlowVO>(`/v1/flow/${id}`),
  create: (data: FlowSave) => post<number>('/v1/flow', data),
  update: (data: FlowSave) => put('/v1/flow', data),
  remove: (id: number) => del(`/v1/flow/${id}`)
}

export const versionApi = {
  list: (flowId: number) => get<FlowVersionVO[]>(`/v1/flow/${flowId}/versions`),
  create: (flowId: number, data: FlowVersionSave) =>
    post<number>(`/v1/flow/${flowId}/versions`, data),
  publish: (flowId: number, versionId: number) =>
    post<void>(`/v1/flow/${flowId}/publish/${versionId}`)
}

export const runApi = {
  run: (id: number, data: { input?: any; async?: boolean }) =>
    post<FlowRunVO>(`/v1/flow/${id}/run`, data),
  runs: (id: number) => get<PageResult<FlowRunVO>>(`/v1/flow/${id}/runs`),
  get: (runId: number) => get<{ run: FlowRunVO; steps: FlowRunStepVO[] }>(`/v1/flow/run/${runId}`),
  testTrigger: (id: number, data: { type: string; config?: any }) =>
    post<{ success: boolean; message?: string }>(`/v1/flow/${id}/trigger/test`, data)
}

export const triggerApi = {
  list: (flowId: number) => get<FlowTriggerVO[]>(`/v1/flow/${flowId}/triggers`),
  create: (flowId: number, data: FlowTriggerSave) =>
    post<number>(`/v1/flow/${flowId}/triggers`, data),
  update: (flowId: number, triggerId: number, data: FlowTriggerSave) =>
    put<void>(`/v1/flow/${flowId}/triggers/${triggerId}`, data),
  remove: (flowId: number, triggerId: number) =>
    del(`/v1/flow/${flowId}/triggers/${triggerId}`)
}

export const nodeDefApi = {
  list: () => get<NodeDefinition[]>('/v1/flow/node-definitions')
}

export const customNodeApi = {
  page: (q: PageQuery) => get<PageResult<CustomNodeVO>>('/v1/flow/custom-nodes', q),
  list: (projectId: number) => get<CustomNodeVO[]>('/v1/flow/custom-nodes', { projectId }),
  create: (data: CustomNodeSave) => post<number>('/v1/flow/custom-nodes', data),
  update: (data: CustomNodeSave) => put<void>('/v1/flow/custom-nodes', data),
  remove: (id: number) => del(`/v1/flow/custom-nodes/${id}`)
}
