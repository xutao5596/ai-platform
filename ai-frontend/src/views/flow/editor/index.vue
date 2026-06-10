<template>
  <div class="flow-editor-page">
    <div class="editor-toolbar">
      <el-button text @click="$router.push('/flow')">
        <el-icon><ArrowLeft /></el-icon> {{ t('flow.editor.back') }}
      </el-button>
      <el-divider direction="vertical" />
      <span class="flow-name">{{ flow?.name || t('flow.editor.loading') }}</span>
      <el-tag v-if="dirty" type="warning" size="small">{{ t('flow.editor.dirty') }}</el-tag>
      <el-tag v-else-if="lastSavedAt" type="success" size="small">{{ t('flow.editor.autosaved') }}</el-tag>
      <div class="toolbar-middle">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('flow.editor.searchPlaceholder')"
          clearable
          size="small"
          style="width: 200px"
          :prefix-icon="Search"
        />
        <span v-if="searchKeyword" class="search-result">{{ t('flow.editor.searchResult', { count: matchCount }) }}</span>
      </div>
      <div class="toolbar-right">
        <el-button :icon="VideoPlay" :loading="running" @click="onRun">{{ t('flow.editor.actionRun') }}</el-button>
        <el-button :icon="Position" @click="onAutoLayout">{{ t('flow.editor.actionLayout') }}</el-button>
        <el-button :icon="RefreshLeft" :disabled="!canUndo" @click="onUndo">{{ t('flow.editor.actionUndo') }}</el-button>
        <el-button :icon="RefreshRight" :disabled="!canRedo" @click="onRedo">{{ t('flow.editor.actionRedo') }}</el-button>
        <el-button :icon="Refresh" @click="onReload">{{ t('flow.editor.actionReload') }}</el-button>
        <el-button type="primary" :icon="Document" :loading="saving" @click="onSave">{{ t('flow.editor.actionSave') }}</el-button>
      </div>
    </div>

    <div class="editor-body">
      <aside class="node-panel" v-loading="loadingDefs">
        <div
          v-for="cat in categories"
          :key="cat.key"
          class="node-category"
        >
          <div class="category-title">
            <el-icon><component :is="cat.icon" /></el-icon>
            <span>{{ t(cat.labelKey) }}</span>
          </div>
          <div class="category-items">
            <div
              v-for="n in nodesByCategory[cat.key]"
              :key="n.typeKey"
              class="node-item"
              :style="{ borderColor: n.color, background: n.color + '14' }"
              :data-type="n.typeKey"
              @mousedown="onDragStart($event, n)"
            >
              <div class="node-item-name" :style="{ color: n.color }">{{ n.displayName }}</div>
              <div class="node-item-desc">{{ n.description }}</div>
            </div>
          </div>
        </div>
      </aside>

      <main class="canvas-wrapper" ref="canvasContainerRef"></main>

      <aside class="prop-panel">
        <div v-if="!selection" class="prop-empty">
          <el-icon size="48" color="#dcdfe6"><InfoFilled /></el-icon>
          <p>{{ t('flow.editor.selectNode') }}</p>
        </div>
        <div v-else class="prop-content">
          <div class="prop-header">
            <span class="prop-title">{{ selectionDisplayName }}</span>
            <el-button text :icon="Delete" @click="onDeleteNode" />
          </div>
          <el-form label-position="top" size="small" class="prop-form">
            <el-form-item :label="t('flow.editor.propIdLabel')">
              <el-input :model-value="selection.typeKey" disabled />
            </el-form-item>
            <el-form-item
              v-for="p in selectionProperties"
              :key="p.key"
              :label="p.label"
              :required="p.required"
            >
              <component
                :is="resolveComponent(p)"
                v-if="resolveComponent(p)"
                v-model="selection.properties[p.key]"
                v-bind="resolveBindings(p)"
                style="width: 100%"
              />
              <el-input
                v-else
                :model-value="selection.properties[p.key]"
                disabled
                :placeholder="t('flow.editor.propUnsupported')"
              />
            </el-form-item>
          </el-form>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  VideoPlay,
  Document,
  Refresh,
  Position,
  Delete,
  InfoFilled,
  MagicStick,
  Cpu,
  Share,
  Connection,
  Operation,
  Search,
  RefreshLeft,
  RefreshRight
} from '@element-plus/icons-vue'
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import MiniMap from '@logicflow/extension/es/components/mini-map'
import '@logicflow/extension/dist/index.css'
import { flowApi, nodeDefApi, runApi } from '@/api/flow'
import { buildNodeViewModel } from '@/components/FlowEditor/nodeFactory'
import { useFlowStore } from '@/store/modules/flow'
import type { FlowVO, NodeDefinition, Property } from '@/types/flow'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const flowStore = useFlowStore()
const flowId = Number(route.params.id)

const flow = ref<FlowVO | null>(null)
const saving = ref(false)
const running = ref(false)
const dirty = ref(false)
const loadingDefs = ref(false)
const lastSavedAt = ref<number | null>(null)
const nodeDefs = ref<NodeDefinition[]>([])
const selection = ref<{
  nodeId: string
  typeKey: string
  properties: Record<string, any>
} | null>(null)
const searchKeyword = ref('')
const matchCount = ref(0)
const canUndo = ref(false)
const canRedo = ref(false)

const canvasContainerRef = ref<HTMLElement | null>(null)
const lfRef = shallowRef<LogicFlow | null>(null)

let autosaveTimer: ReturnType<typeof setTimeout> | null = null
let initialLoaded = false

const categories = [
  { key: 'basic' as const, labelKey: 'flow.editor.categoryBasic', icon: Cpu },
  { key: 'ai' as const, labelKey: 'flow.editor.categoryAi', icon: MagicStick },
  { key: 'control' as const, labelKey: 'flow.editor.categoryControl', icon: Share },
  { key: 'tool' as const, labelKey: 'flow.editor.categoryTool', icon: Connection },
  { key: 'data' as const, labelKey: 'flow.editor.categoryData', icon: Operation }
]

const nodesByCategory = computed<Record<string, NodeDefinition[]>>(() => {
  const map: Record<string, NodeDefinition[]> = {
    basic: [], ai: [], control: [], tool: [], data: []
  }
  for (const n of nodeDefs.value) {
    if (map[n.category]) map[n.category].push(n)
  }
  return map
})

const selectionDisplayName = computed(() => {
  if (!selection.value) return ''
  const def = nodeDefs.value.find(n => n.typeKey === selection.value!.typeKey)
  return def?.displayName || selection.value.typeKey
})

const selectionProperties = computed<Property[]>(() => {
  if (!selection.value) return []
  const def = nodeDefs.value.find(n => n.typeKey === selection.value!.typeKey)
  if (!def) return []
  return Array.isArray(def.inputs) ? def.inputs : parseInputs(def.inputs as any)
})

function resolveComponent(p: Property): any {
  switch (p.type) {
    case 'string': return 'el-input'
    case 'number': return 'el-input-number'
    case 'boolean': return 'el-switch'
    case 'select': return 'el-select'
    case 'textarea': return 'el-input'
    case 'json': return 'el-input'
    case 'model': return 'el-select'
    case 'kb': return 'el-select'
    case 'prompt': return 'el-select'
    case 'mcp': return 'el-select'
    default: return null
  }
}

function resolveBindings(p: Property): Record<string, any> {
  const b: Record<string, any> = {}
  if (p.type === 'textarea') {
    b.type = 'textarea'
    b.rows = 3
  }
  if (p.type === 'json') {
    b.type = 'textarea'
    b.rows = 4
    b.placeholder = 'JSON'
  }
  if (p.type === 'number') {
    b.min = 0
  }
  return b
}

async function loadFlow() {
  flow.value = await flowApi.get(flowId)
  flowStore.setCurrentFlowId(flowId)
}

async function loadNodeDefs() {
  loadingDefs.value = true
  try {
    nodeDefs.value = await nodeDefApi.list()
    flowStore.nodeDefs = nodeDefs.value
    flowStore.nodeDefsLoaded = true
  } finally {
    loadingDefs.value = false
  }
}

function buildLfOptions() {
  return {
    container: canvasContainerRef.value as HTMLElement,
    grid: true,
    background: { backgroundColor: '#fafbfc' },
    keyboard: { enabled: true },
    plugins: [MiniMap]
  }
}

function applyNodeHighlights() {
  const lf = lfRef.value
  if (!lf) return
  const kw = searchKeyword.value.trim().toLowerCase()
  const nodes = lf.graphModel.nodes
  matchCount.value = 0
  for (const node of nodes) {
    if (!kw) {
      node.setProperty('_highlight', false)
      continue
    }
    const def = nodeDefs.value.find(n => n.typeKey === (node as any).type)
    const displayName = (def?.displayName || (node as any).type || '').toLowerCase()
    const id = (node.id || '').toLowerCase()
    const text = ((node as any).text?.value || (typeof (node as any).text === 'string' ? (node as any).text : '') || '').toLowerCase()
    const hit = displayName.includes(kw) || id.includes(kw) || text.includes(kw)
    node.setProperty('_highlight', hit)
    if (hit) matchCount.value++
  }
}

function onPropertyChange() {
  if (!lfRef.value) return
  applyNodeHighlights()
}

function initLogicFlow(initialData?: any) {
  if (!canvasContainerRef.value) return
  if (lfRef.value) {
    lfRef.value.destroy()
    lfRef.value = null
  }
  const lf = new LogicFlow(buildLfOptions())
  lf.setDefaultEdgeType('polyline')

  // Register every node type from backend definitions (Jeecg-style HTML node)
  const registered = new Set<string>()
  for (const def of nodeDefs.value) {
    if (registered.has(def.typeKey)) continue
    try {
      const { Model, View } = buildNodeViewModel(def)
      lf.register({
        type: def.typeKey,
        view: View as any,
        model: Model as any
      })
      registered.add(def.typeKey)
    } catch (e) {
      console.warn('[flow-editor] failed to register', def.typeKey, e)
    }
  }
  // Aliases: tolerate legacy type names
  const aliases: Record<string, string> = { ifelse: 'if_else' }
  for (const [alias, canonical] of Object.entries(aliases)) {
    if (registered.has(canonical) && !registered.has(alias)) {
      const def = nodeDefs.value.find(n => n.typeKey === canonical)
      if (def) {
        try {
          const { Model, View } = buildNodeViewModel({ ...def, typeKey: alias })
          lf.register({ type: alias, view: View as any, model: Model as any })
          registered.add(alias)
        } catch { /* ignore */ }
      }
    }
  }

  lfRef.value = lf

  lf.on('node:click', ({ data }: any) => {
    const def = nodeDefs.value.find(n => n.typeKey === data.type)
    const properties: Record<string, any> = {}
    const inputs = def ? (Array.isArray(def.inputs) ? def.inputs : parseInputs(def.inputs)) : []
    if (def) {
      for (const p of inputs) {
        properties[p.key] = data.properties?.[p.key] ?? p.default ?? defaultFor(p)
      }
    } else {
      Object.assign(properties, data.properties || {})
    }
    // Stable display label from properties or fallback
    if (!properties._label) properties._label = data.properties?._label || def?.displayName || data.type
    if (!properties._sub) properties._sub = data.properties?._sub || def?.description || ''
    selection.value = {
      nodeId: data.id,
      typeKey: data.type,
      properties
    }
    flowStore.setSelection(selection.value)
  })

  lf.on('edge:click', () => {
    selection.value = null
    flowStore.setSelection(null)
  })

  lf.on('blank:click', () => {
    selection.value = null
    flowStore.setSelection(null)
  })

  lf.on('history:change', () => {
    if (!dirty.value) {
      dirty.value = true
      flowStore.markDirty()
    }
    updateHistoryButtons()
    applyNodeHighlights()
    scheduleAutoSave()
  })

  lf.on('node:drag', () => { dirty.value = true; scheduleAutoSave() })
  lf.on('edge:add', () => { dirty.value = true; scheduleAutoSave() })

  // Normalize incoming data: strip system properties & keep _label/_sub on existing nodes
  const data = normalizeDesignData(initialData, nodeDefs.value)
  if (data.nodes.length > 0) {
    lf.render(data)
  } else {
    lf.render({ nodes: [], edges: [] })
  }
  updateHistoryButtons()
  applyNodeHighlights()
}

function parseInputs(raw: any): Property[] {
  // Backend sometimes serializes List<Property> via Object.toString ("@{...}")
  // Frontend fallback: try to parse and recover fields. If still impossible,
  // return an empty array (generic form will be used).
  if (Array.isArray(raw)) return raw
  if (typeof raw !== 'string') return []
  const out: Property[] = []
  const re = /@\{([\s\S]*?)\}/g
  let m: RegExpExecArray | null
  while ((m = re.exec(raw)) !== null) {
    const body = m[1]
    const obj: any = {}
    body.split(/[,;]\s*(?=[a-zA-Z_])/).forEach((kv) => {
      const idx = kv.indexOf('=')
      if (idx < 0) return
      const k = kv.substring(0, idx).trim()
      let v: any = kv.substring(idx + 1).trim()
      if (v === 'True' || v === 'true') v = true
      else if (v === 'False' || v === 'false') v = false
      else if (/^-?\d+(\.\d+)?$/.test(v)) v = Number(v)
      else if (v.startsWith('System.') || v.startsWith('[') || v.startsWith('{')) v = v // leave as-is
      obj[k] = v
    })
    if (obj.key) out.push(obj as Property)
  }
  return out
}

function normalizeDesignData(raw: any, defs: NodeDefinition[]) {
  const empty = { nodes: [] as any[], edges: [] as any[] }
  if (!raw || typeof raw !== 'object') return empty
  let nodes: any[] = []
  let edges: any[] = []
  if (Array.isArray(raw.nodes)) nodes = raw.nodes
  if (Array.isArray(raw.edges)) edges = raw.edges
  if (nodes.length === 0 && typeof raw.nodes === 'string') {
    try { nodes = JSON.parse(raw.nodes) } catch { nodes = [] }
  }
  if (edges.length === 0 && typeof raw.edges === 'string') {
    try { edges = JSON.parse(raw.edges) } catch { edges = [] }
  }
  for (const n of nodes) {
    const def = defs.find(d => d.typeKey === n.type)
    if (def) {
      n.properties = n.properties || {}
      if (!n.properties._label) n.properties._label = n.text?.value || def.displayName
      if (!n.properties._sub) n.properties._sub = def.description || ''
    }
  }
  return { nodes, edges }
}

function defaultFor(p: Property): any {
  switch (p.type) {
    case 'number': return 0
    case 'boolean': return false
    case 'json': return '{}'
    default: return ''
  }
}

function updateHistoryButtons() {
  if (!lfRef.value) {
    canUndo.value = false
    canRedo.value = false
    return
  }
  const h: any = (lfRef.value as any).history
  if (!h) {
    canUndo.value = false
    canRedo.value = false
    return
  }
  canUndo.value = !!(h.undoAble && h.undoAble())
  canRedo.value = !!(h.redoAble && h.redoAble())
}

function onUndo() {
  if (!lfRef.value) return
  ;(lfRef.value as any).undo()
  updateHistoryButtons()
  applyNodeHighlights()
  dirty.value = true
  flowStore.markDirty()
}

function onRedo() {
  if (!lfRef.value) return
  ;(lfRef.value as any).redo()
  updateHistoryButtons()
  applyNodeHighlights()
  dirty.value = true
  flowStore.markDirty()
}

function onDragStart(ev: MouseEvent, n: NodeDefinition) {
  ev.preventDefault()
  if (!lfRef.value) return
  const target = ev.currentTarget as HTMLElement
  lfRef.value.dnd.startDrag({
    type: n.typeKey,
    text: n.displayName,
    properties: { typeKey: n.typeKey },
    sourceNode: target as any
  })
}

async function persist(design: any) {
  if (!flow.value) return
  await flowApi.update({
    id: flowId,
    projectId: flow.value.projectId || 0,
    name: flow.value.name || '',
    description: flow.value.description,
    isAssistant: flow.value.isAssistant,
    status: flow.value.status,
    design: typeof design === 'string' ? design : JSON.stringify(design)
  })
}

async function onSave() {
  if (!lfRef.value) return
  saving.value = true
  try {
    const data = lfRef.value.getGraphData()
    const design = JSON.parse(JSON.stringify(data))
    await persist(design)
    dirty.value = false
    lastSavedAt.value = Date.now()
    flowStore.markClean()
    ElMessage.success(t('common.save'))
  } catch {
    ElMessage.error(t('flow.editor.saveFailed'))
  } finally {
    saving.value = false
  }
}

function scheduleAutoSave() {
  if (!initialLoaded) return
  if (autosaveTimer) clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(async () => {
    if (!lfRef.value || !dirty.value) return
    try {
      const data = lfRef.value.getGraphData()
      const design = JSON.parse(JSON.stringify(data))
      await persist(design)
      dirty.value = false
      lastSavedAt.value = Date.now()
      flowStore.markClean()
    } catch (e) {
      // keep dirty so user can retry manually
    }
  }, 3000)
}

async function onReload() {
  if (dirty.value) {
    await ElMessageBox.confirm(t('flow.editor.reloadConfirm'), t('common.confirm'), { type: 'warning' })
      .catch(() => null)
      .then(r => { if (!r) throw new Error('cancel') })
  }
  if (autosaveTimer) {
    clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
  await loadFlow()
  initLogicFlow(flow.value?.design)
  selection.value = null
  dirty.value = false
  ElMessage.success(t('flow.editor.reloaded'))
}

function onAutoLayout() {
  ElMessage.info(t('flow.editor.layoutTip'))
}

async function onRun() {
  await onSave()
  running.value = true
  try {
    const { value: inputJson } = await ElMessageBox.prompt(
      t('flow.editor.runDialogTitle'),
      t('flow.editor.runDialogTitle'),
      { inputPlaceholder: t('flow.editor.runInputPlaceholder'), inputType: 'textarea' }
    ).catch(() => ({ value: '{}' }))
    let input: any
    try {
      input = inputJson ? JSON.parse(inputJson) : {}
    } catch {
      ElMessage.error(t('flow.editor.runJsonError'))
      return
    }
    const run = await runApi.run(flowId, { input })
    ElMessage.success(t('flow.editor.runSuccess', { status: run.status, cost: run.costMs ?? 0 }))
  } catch (e: any) {
    if (e?.message !== 'cancel') ElMessage.error(t('flow.editor.runFailed'))
  } finally {
    running.value = false
  }
}

function onDeleteNode() {
  if (!lfRef.value || !selection.value) return
  lfRef.value.deleteNode(selection.value.nodeId)
  selection.value = null
  flowStore.setSelection(null)
  dirty.value = true
}

function onKeydown(e: KeyboardEvent) {
  const isCtrl = e.ctrlKey || e.metaKey
  if (!isCtrl) return
  const key = e.key.toLowerCase()
  if (key === 'z' && !e.shiftKey) {
    e.preventDefault()
    onUndo()
  } else if ((key === 'z' && e.shiftKey) || key === 'y') {
    e.preventDefault()
    onRedo()
  }
}

watch(searchKeyword, () => {
  applyNodeHighlights()
})

watch(selection, () => {
  if (selection.value && lfRef.value) {
    const lf = lfRef.value as any
    const node = lf.getNodeModelById?.(selection.value.nodeId)
    if (node && typeof node.setProperty === 'function') {
      // re-apply property changes to node properties (only for ones user edited)
    }
  }
}, { deep: true })

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  await loadNodeDefs()
  await loadFlow()
  await nextTick()
  initLogicFlow(flow.value?.design)
  initialLoaded = true
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if (autosaveTimer) {
    clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
  if (lfRef.value) {
    lfRef.value.destroy()
    lfRef.value = null
  }
  flowStore.setSelection(null)
})
</script>

<style scoped>
.flow-editor-page {
  display: flex; flex-direction: column;
  height: calc(100vh - 56px - 32px);
  margin: -16px;
  background: #fff;
}
.editor-toolbar {
  height: 48px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px;
  padding: 0 12px;
  border-bottom: 1px solid var(--ai-border);
  background: #fff;
}
.flow-name { font-weight: 600; font-size: 15px; }
.toolbar-middle {
  display: flex; align-items: center; gap: 8px;
  margin-left: 16px;
}
.search-result { font-size: 12px; color: var(--ai-text-secondary); }
.toolbar-right { margin-left: auto; display: flex; gap: 8px; }
.editor-body { flex: 1; display: flex; min-height: 0; }

.node-panel {
  width: 240px; flex-shrink: 0;
  border-right: 1px solid var(--ai-border);
  background: #f5f7fa;
  overflow-y: auto;
  padding: 8px 0;
}
.node-category { margin-bottom: 8px; }
.category-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: var(--ai-text-secondary);
  padding: 6px 12px;
}
.category-items { display: flex; flex-direction: column; gap: 4px; padding: 0 8px; }
.node-item {
  padding: 8px 10px;
  border: 1px solid;
  border-radius: 6px;
  cursor: grab;
  user-select: none;
  transition: transform 0.1s;
}
.node-item:hover { transform: translateX(2px); }
.node-item:active { cursor: grabbing; }
.node-item-name { font-size: 13px; font-weight: 600; margin-bottom: 2px; }
.node-item-desc {
  font-size: 11px; color: var(--ai-text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.canvas-wrapper { flex: 1; min-width: 0; }
.canvas-wrapper :deep(.lf-node) {
  transition: filter 0.15s, stroke-width 0.15s;
}
.canvas-wrapper :deep(.lf-node[data-highlight="true"]) {
  filter: drop-shadow(0 0 4px #409eff) drop-shadow(0 0 2px #409eff);
}
.canvas-wrapper :deep(.lf-node[data-highlight="false"]) {
  opacity: 0.45;
}

.prop-panel {
  width: 320px; flex-shrink: 0;
  border-left: 1px solid var(--ai-border);
  background: #fff;
  overflow-y: auto;
}
.prop-empty { text-align: center; padding-top: 80px; color: var(--ai-text-secondary); }
.prop-content { padding: 12px; }
.prop-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 8px; margin-bottom: 8px;
  border-bottom: 1px solid var(--ai-border);
}
.prop-title { font-size: 15px; font-weight: 600; }
.prop-form :deep(.el-form-item) { margin-bottom: 12px; }
.prop-form :deep(.el-form-item__label) { font-weight: 500; padding-bottom: 2px; }
</style>
