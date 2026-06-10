<template>
  <div class="flow-editor-page">
    <div class="editor-toolbar">
      <el-button text @click="$router.push('/flow')">
        <el-icon><ArrowLeft /></el-icon> {{ t('flow.editor.back') }}
      </el-button>
      <el-divider direction="vertical" />
      <span class="flow-name">{{ flow?.name || t('flow.editor.loading') }}</span>
      <el-tag v-if="dirty" type="warning" size="small">{{ t('flow.editor.dirty') }}</el-tag>
      <div class="toolbar-right">
        <el-button :icon="VideoPlay" :loading="running" @click="onRun">{{ t('flow.editor.actionRun') }}</el-button>
        <el-button :icon="Position" @click="onAutoLayout">{{ t('flow.editor.actionLayout') }}</el-button>
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick, shallowRef } from 'vue'
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
  Operation
} from '@element-plus/icons-vue'
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import { MiniMap } from '@logicflow/extension'
import '@logicflow/extension/dist/index.css'
import { flowApi, nodeDefApi, runApi } from '@/api/flow'
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
const nodeDefs = ref<NodeDefinition[]>([])
const selection = ref<{
  nodeId: string
  typeKey: string
  properties: Record<string, any>
} | null>(null)

const canvasContainerRef = ref<HTMLElement | null>(null)
const lfRef = shallowRef<LogicFlow | null>(null)

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
  return def?.inputs || []
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
  if (p.type === 'select' || p.type === 'model' || p.type === 'kb' || p.type === 'prompt' || p.type === 'mcp') {
    if (p.type === 'select' && p.options) {
      // render manually below
    }
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
    keyboard: { enabled: true }
  }
}

function initLogicFlow(initialData?: any) {
  if (!canvasContainerRef.value) return
  if (lfRef.value) {
    lfRef.value.destroy()
    lfRef.value = null
  }
  const lf = new LogicFlow(buildLfOptions())
  lf.use(MiniMap)
  lf.setDefaultEdgeType('polyline')
  lfRef.value = lf

  lf.on('node:click', ({ data }: any) => {
    const def = nodeDefs.value.find(n => n.typeKey === data.type)
    const properties: Record<string, any> = {}
    if (def) {
      for (const p of def.inputs) {
        properties[p.key] = data.properties?.[p.key] ?? p.default ?? defaultFor(p)
      }
    } else {
      Object.assign(properties, data.properties || {})
    }
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
  })

  lf.on('node:drag', () => { dirty.value = true })
  lf.on('edge:add', () => { dirty.value = true })

  const empty = { nodes: [], edges: [] }
  if (initialData && initialData.nodes && initialData.nodes.length > 0) {
    lf.render(initialData)
  } else {
    lf.render(empty)
  }
}

function defaultFor(p: Property): any {
  switch (p.type) {
    case 'number': return 0
    case 'boolean': return false
    case 'json': return '{}'
    default: return ''
  }
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

async function onSave() {
  if (!lfRef.value) return
  saving.value = true
  try {
    const data = lfRef.value.getGraphData()
    const design = JSON.parse(JSON.stringify(data))
    await flowApi.update({
      id: flowId,
      projectId: flow.value?.projectId || 0,
      name: flow.value?.name || '',
      description: flow.value?.description,
      isAssistant: flow.value?.isAssistant,
      status: flow.value?.status,
      design
    })
    dirty.value = false
    flowStore.markClean()
    ElMessage.success(t('common.save'))
  } catch {
    ElMessage.error(t('flow.editor.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function onReload() {
  if (dirty.value) {
    await ElMessageBox.confirm(t('flow.editor.reloadConfirm'), t('common.confirm'), { type: 'warning' })
      .catch(() => null)
      .then(r => { if (!r) throw new Error('cancel') })
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

onMounted(async () => {
  await loadNodeDefs()
  await loadFlow()
  await nextTick()
  initLogicFlow(flow.value?.design)
})

onBeforeUnmount(() => {
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
