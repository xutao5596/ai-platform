<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">流程列表</span>
      <div class="header-right">
        <el-select
          v-model="currentProjectId"
          placeholder="选择项目"
          style="width: 220px"
          @change="onProjectChange"
        >
          <el-option
            v-for="p in projects"
            :key="p.id"
            :value="p.id"
            :label="p.name"
          />
        </el-select>
        <el-button v-if="currentProjectId" type="primary" :icon="Plus" @click="onAdd">
          新建流程
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="流程名"
        clearable
        @keyup.enter="reload"
        style="width: 240px"
      />
      <el-select v-model="query.isAssistant" placeholder="类型" clearable style="width: 140px">
        <el-option :value="0" label="普通流程" />
        <el-option :value="1" label="AI 助手" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="goDetail(row as FlowVO)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isAssistant === 1" type="warning" size="small">助手</el-tag>
          <el-tag v-else type="info" size="small">流程</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goEditor(row as FlowVO)">编辑</el-button>
          <el-button size="small" @click="onVersions(row as FlowVO)">版本</el-button>
          <el-button size="small" @click="onTriggers(row as FlowVO)">触发器</el-button>
          <el-button size="small" type="success" @click="onRun(row as FlowVO)">运行</el-button>
          <el-button size="small" type="danger" @click="onDelete(row as FlowVO)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!currentProjectId" description="请先选择项目" />
    <el-empty v-else-if="!loading && rows.length === 0" description="暂无流程,点击右上角创建" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑流程' : '新建流程'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.isAssistant">
            <el-radio :value="0">普通流程</el-radio>
            <el-radio :value="1">AI 助手</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionsDrawer" :title="`版本管理 — ${currentFlow?.name || ''}`" direction="rtl" size="55%">
      <div class="versions-content">
        <el-button type="primary" :icon="Plus" @click="onCreateVersion" style="margin-bottom: 12px">
          新建版本
        </el-button>
        <el-table :data="versions" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="version" label="版本号" width="100" />
          <el-table-column prop="changelog" label="变更说明" />
          <el-table-column label="当前" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="success" size="small">激活</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.isActive !== 1" size="small" type="primary" @click="onPublish(row as FlowVersionVO)">
                发布
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer v-model="triggersDrawer" :title="`触发器 — ${currentFlow?.name || ''}`" direction="rtl" size="55%">
      <div class="triggers-content">
        <el-button type="primary" :icon="Plus" @click="onAddTrigger" style="margin-bottom: 12px">
          新建触发器
        </el-button>
        <el-table :data="triggers" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ triggerLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" />
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" disabled />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="onDeleteTrigger(row as FlowTriggerVO)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="triggerDialogVisible" :title="triggerForm.id ? '编辑触发器' : '新建触发器'" width="480px">
      <el-form :model="triggerForm" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="triggerForm.type" style="width: 100%">
            <el-option value="manual" label="手动" />
            <el-option value="cron" label="定时" />
            <el-option value="webhook" label="Webhook" />
            <el-option value="event" label="事件" />
            <el-option value="chained" label="级联" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="triggerForm.description" />
        </el-form-item>
        <el-form-item label="配置 (JSON)">
          <el-input v-model="triggerConfigJson" type="textarea" :rows="4" placeholder='{"cron":"0 0 * * *"}' />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="triggerForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveTrigger">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAppStore } from '@/store/modules/app'
import { flowApi, versionApi, triggerApi, runApi } from '@/api/flow'
import { projectApi, type ProjectVO } from '@/api/project'
import type { FlowVO, FlowSave, FlowVersionVO, FlowTriggerVO, FlowTriggerSave, TriggerType } from '@/types/flow'

const router = useRouter()
const appStore = useAppStore()

const projects = ref<ProjectVO[]>([])
const currentProjectId = ref<number | null>(appStore.currentProjectId)
const query = ref<{ keyword: string; isAssistant?: number; current: number; size: number }>({
  keyword: '',
  isAssistant: undefined,
  current: 1,
  size: 20
})
const total = ref(0)
const rows = ref<FlowVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const form = ref<FlowSave>({ projectId: 0, name: '', description: '', isAssistant: 0 })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

const versionsDrawer = ref(false)
const versions = ref<FlowVersionVO[]>([])
const currentFlow = ref<FlowVO | null>(null)

const triggersDrawer = ref(false)
const triggers = ref<FlowTriggerVO[]>([])
const triggerDialogVisible = ref(false)
const triggerForm = ref<FlowTriggerSave & { id?: number }>({ type: 'manual', enabled: 1, description: '' })
const triggerConfigJson = ref('')

function triggerLabel(t: TriggerType) {
  return ({ manual: '手动', cron: '定时', webhook: 'Webhook', event: '事件', chained: '级联' } as const)[t] || t
}

async function loadProjects() {
  try {
    projects.value = await projectApi.mine()
  } catch {
    projects.value = []
  }
  if (projects.value.length > 0 && !currentProjectId.value) {
    currentProjectId.value = projects.value[0].id
    appStore.setCurrentProject(currentProjectId.value)
  }
}

function onProjectChange(v: number | null) {
  appStore.setCurrentProject(v)
  reload()
}

async function reload() {
  if (!currentProjectId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res = await flowApi.page({
      projectId: currentProjectId.value,
      keyword: query.value.keyword,
      isAssistant: query.value.isAssistant,
      current: query.value.current,
      size: query.value.size
    })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  if (!currentProjectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  form.value = { projectId: currentProjectId.value, name: '', description: '', isAssistant: 0 }
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  if (form.value.id) {
    await flowApi.update(form.value)
    ElMessage.success('已更新')
  } else {
    await flowApi.create(form.value)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  reload()
}

function goEditor(row: FlowVO) {
  router.push(`/flow/${row.id}/editor`)
}
function goDetail(row: FlowVO) {
  router.push(`/flow/${row.id}`)
}

async function onVersions(row: FlowVO) {
  currentFlow.value = row
  versions.value = await versionApi.list(row.id)
  versionsDrawer.value = true
}

async function onCreateVersion() {
  if (!currentFlow.value) return
  const { value: changelog } = await ElMessageBox.prompt('请输入变更说明', '新建版本', {
    inputPlaceholder: '例如: 新增 LLM 节点'
  }).catch(() => ({ value: '' }))
  if (changelog === '') return
  await versionApi.create(currentFlow.value.id, {
    design: currentFlow.value.design || { nodes: [], edges: [] },
    changelog
  })
  ElMessage.success('已创建版本')
  versions.value = await versionApi.list(currentFlow.value.id)
}

async function onPublish(v: FlowVersionVO) {
  if (!currentFlow.value) return
  await versionApi.publish(currentFlow.value.id, v.id)
  ElMessage.success(`已发布 v${v.version}`)
  versions.value = await versionApi.list(currentFlow.value.id)
}

async function onTriggers(row: FlowVO) {
  currentFlow.value = row
  triggers.value = await triggerApi.list(row.id)
  triggersDrawer.value = true
}

function onAddTrigger() {
  triggerForm.value = { type: 'manual', enabled: 1, description: '' }
  triggerConfigJson.value = ''
  triggerDialogVisible.value = true
}

async function onSaveTrigger() {
  if (!currentFlow.value) return
  let config: any
  if (triggerConfigJson.value.trim()) {
    try {
      config = JSON.parse(triggerConfigJson.value)
    } catch {
      ElMessage.error('配置 JSON 格式错误')
      return
    }
  }
  await triggerApi.create(currentFlow.value.id, { ...triggerForm.value, config })
  triggerDialogVisible.value = false
  ElMessage.success('已创建')
  triggers.value = await triggerApi.list(currentFlow.value.id)
}

async function onDeleteTrigger(t: FlowTriggerVO) {
  await ElMessageBox.confirm(`删除触发器 [${t.type}]?`, '确认', { type: 'warning' })
  if (!currentFlow.value) return
  await triggerApi.remove(currentFlow.value.id, t.id)
  ElMessage.success('已删除')
  triggers.value = await triggerApi.list(currentFlow.value.id)
}

async function onRun(row: FlowVO) {
  const { value: inputJson } = await ElMessageBox.prompt('输入 JSON (可空)', '运行流程', {
    inputPlaceholder: '{}',
    inputType: 'textarea'
  }).catch(() => ({ value: '{}' }))
  let input: any
  try {
    input = inputJson ? JSON.parse(inputJson) : {}
  } catch {
    ElMessage.error('JSON 格式错误')
    return
  }
  try {
    const run = await runApi.run(row.id, { input })
    ElMessage.success(`运行完成: ${run.status} (${run.costMs ?? 0}ms)`)
  } catch {
    ElMessage.error('运行失败')
  }
}

async function onDelete(row: FlowVO) {
  await ElMessageBox.confirm(`删除流程 [${row.name}]?`, '确认', { type: 'warning' })
  await flowApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  await loadProjects()
  await reload()
})
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.header-right { display: flex; align-items: center; gap: 12px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.pager { margin-top: 12px; text-align: right; }
</style>
