export type NodeCategory = 'basic' | 'ai' | 'control' | 'tool' | 'data'

export type PropertyType =
  | 'string'
  | 'number'
  | 'boolean'
  | 'select'
  | 'json'
  | 'textarea'
  | 'model'
  | 'kb'
  | 'prompt'
  | 'mcp'

export interface Property {
  key: string
  label: string
  type: PropertyType
  required?: boolean
  default?: any
  options?: { label: string; value: any }[]
  description?: string
}

export interface NodeDefinition {
  typeKey: string
  category: NodeCategory
  displayName: string
  description: string
  icon: string
  color: string
  inputs: Property[]
  outputs: Property[]
}

export interface FlowVO {
  id: number
  projectId: number
  name: string
  description?: string
  isAssistant: number
  status: number
  version?: string
  currentVersionId?: number
  design?: any
  chain?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface FlowSave {
  id?: number
  projectId: number
  name: string
  description?: string
  isAssistant?: number
  status?: number
  design?: any
  chain?: string
}

export interface FlowVersionVO {
  id: number
  flowId: number
  version: string
  design?: any
  chain?: string
  changelog?: string
  isActive: number
  createTime?: string
}

export interface FlowVersionSave {
  design: any
  chain?: string
  changelog?: string
}

export type RunStatus = 'pending' | 'running' | 'success' | 'failed' | 'cancelled'

export interface FlowRunVO {
  id: number
  flowId: number
  status: RunStatus
  input?: any
  output?: any
  errorMsg?: string
  costMs?: number
  triggerType?: string
  startedAt?: string
  finishedAt?: string
  createTime?: string
}

export interface FlowRunStepVO {
  id: number
  runId: number
  nodeId?: string
  nodeType?: string
  nodeName?: string
  status: RunStatus
  input?: any
  output?: any
  errorMsg?: string
  costMs?: number
  seq?: number
  startedAt?: string
  finishedAt?: string
}

export type TriggerType = 'manual' | 'cron' | 'webhook' | 'event' | 'chained'

export interface FlowTriggerVO {
  id: number
  flowId: number
  type: TriggerType
  config?: any
  enabled: number
  description?: string
  createTime?: string
}

export interface FlowTriggerSave {
  type: TriggerType
  config?: any
  enabled?: number
  description?: string
}

export interface CustomNodeVO {
  id: number
  projectId: number
  typeKey: string
  displayName: string
  category: NodeCategory
  implType: 'http' | 'script' | 'sql'
  config?: any
  description?: string
  status: number
  createTime?: string
}

export interface CustomNodeSave {
  id?: number
  projectId: number
  typeKey: string
  displayName: string
  category: NodeCategory
  implType: 'http' | 'script' | 'sql'
  config?: any
  description?: string
  status?: number
}
