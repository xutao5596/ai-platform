import { defineStore } from 'pinia'
import type { NodeDefinition } from '@/types/flow'
import { nodeDefApi } from '@/api/flow'

interface FlowState {
  currentFlowId: number | null
  currentNodeSelection: {
    nodeId: string
    typeKey: string
    properties: Record<string, any>
  } | null
  nodeDefs: NodeDefinition[]
  nodeDefsLoaded: boolean
  dirty: boolean
  lastSavedAt: number | null
}

export const useFlowStore = defineStore('flow', {
  state: (): FlowState => ({
    currentFlowId: null,
    currentNodeSelection: null,
    nodeDefs: [],
    nodeDefsLoaded: false,
    dirty: false,
    lastSavedAt: null
  }),
  actions: {
    setCurrentFlowId(id: number | null) {
      this.currentFlowId = id
    },
    setSelection(sel: FlowState['currentNodeSelection']) {
      this.currentNodeSelection = sel
    },
    markDirty() {
      this.dirty = true
    },
    markClean() {
      this.dirty = false
      this.lastSavedAt = Date.now()
    },
    async loadNodeDefs(force = false) {
      if (this.nodeDefsLoaded && !force) return
      this.nodeDefs = await nodeDefApi.list()
      this.nodeDefsLoaded = true
    }
  }
})
