<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('flow.list.title') }}</span>
      <div class="header-right">
        <el-select
          v-model="currentProjectId"
          :placeholder="t('flow.list.selectProject')"
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
          {{ t('flow.list.add') }}
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        :placeholder="t('flow.list.searchPlaceholder')"
        clearable
        @keyup.enter="reload"
        style="width: 240px"
      />
      <el-select v-model="query.isAssistant" :placeholder="t('flow.list.typeFilter')" clearable style="width: 140px">
        <el-option :value="0" :label="t('flow.list.typeNormal')" />
        <el-option :value="1" :label="t('flow.list.typeAssistant')" />
      </el-select>
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('flow.list.colName')" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="goDetail(row as FlowVO)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('flow.list.colDesc')" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column :label="t('flow.list.colType')" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isAssistant === 1" type="warning" size="small">{{ t('flow.list.typeTagAssistant') }}</el-tag>
          <el-tag v-else type="info" size="small">{{ t('flow.list.typeTagNormal') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? t('flow.list.statusPublished') : t('flow.list.statusDraft') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('flow.list.colVersion')" prop="version" width="100" />
      <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goEditor(row as FlowVO)">{{ t('flow.list.actionEdit') }}</el-button>
          <el-button size="small" @click="onVersions(row as FlowVO)">{{ t('flow.list.actionVersions') }}</el-button>
          <el-button size="small" @click="onTriggers(row as FlowVO)">{{ t('flow.list.actionTriggers') }}</el-button>
          <el-button size="small" type="success" @click="onRun(row as FlowVO)">{{ t('flow.list.actionRun') }}</el-button>
          <el-button size="small" type="danger" @click="onDelete(row as FlowVO)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!currentProjectId" :description="t('flow.list.selectProjectFirst')" />
    <el-empty v-else-if="!loading && rows.length === 0" :description="t('flow.list.emptyTip')" />

    <el-dialog v-model="dialogVisible" :title="form.id ? t('flow.list.editDialogTitle') : t('flow.list.addDialogTitle')" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('flow.list.formName')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('flow.list.formDesc')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('flow.list.formType')">
          <el-radio-group v-model="form.isAssistant">
            <el-radio :value="0">{{ t('flow.list.typeNormal') }}</el-radio>
            <el-radio :value="1">{{ t('flow.list.typeAssistant') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionsDrawer" :title="t('flow.list.versionsTitle', { name: currentFlow?.name || '' })" direction="rtl" size="55%">
      <div class="versions-content">
        <el-button type="primary" :icon="Plus" @click="onCreateVersion" style="margin-bottom: 12px">
          {{ t('flow.list.newVersion') }}
        </el-button>
        <el-table :data="versions" border>
          <el-table-column :label="t('flow.list.colId')" prop="id" width="60" />
          <el-table-column :label="t('flow.list.colVersionNo')" prop="version" width="100" />
          <el-table-column :label="t('flow.list.colChangelog')" prop="changelog" />
          <el-table-column :label="t('ai.prompt.colActive')" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="success" size="small">{{ t('ai.prompt.activeTag') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.isActive !== 1" size="small" type="primary" @click="onPublish(row as FlowVersionVO)">
                {{ t('flow.list.publish') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer v-model="triggersDrawer" :title="t('flow.list.triggersTitle', { name: currentFlow?.name || '' })" direction="rtl" size="55%">
      <div class="triggers-content">
        <el-button type="primary" :icon="Plus" @click="onAddTrigger" style="margin-bottom: 12px">
          {{ t('flow.list.newTrigger') }}
        </el-button>
        <el-table :data="triggers" border>
          <el-table-column :label="t('common.id')" prop="id" width="60" />
          <el-table-column :label="t('flow.list.colTriggerType')" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ t('flow.list.trigger' + (row.type.charAt(0).toUpperCase() + row.type.slice(1))) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('flow.list.colDesc')" prop="description" />
          <el-table-column :label="t('flow.list.colEnabled')" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" disabled />
            </template>
          </el-table-column>
          <el-table-column :label="t('flow.list.colTriggerCreateTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="onDeleteTrigger(row as FlowTriggerVO)">{{ t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="triggerDialogVisible" :title="triggerForm.id ? t('flow.list.editTriggerTitle') : t('flow.list.newTriggerTitle')" width="480px">
      <el-form :model="triggerForm" label-width="100px">
        <el-form-item :label="t('flow.list.triggerFormType')">
          <el-select v-model="triggerForm.type" style="width: 100%">
            <el-option value="manual" :label="t('flow.list.triggerManual')" />
            <el-option value="cron" :label="t('flow.list.triggerCron')" />
            <el-option value="webhook" :label="t('flow.list.triggerWebhook')" />
            <el-option value="event" :label="t('flow.list.triggerEvent')" />
            <el-option value="chained" :label="t('flow.list.triggerChained')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('flow.list.triggerFormDesc')">
          <el-input v-model="triggerForm.description" />
        </el-form-item>
        <el-form-item :label="t('flow.list.formConfig')">
          <el-input v-model="triggerConfigJson" type="textarea" :rows="4" :placeholder="t('flow.list.formConfigPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('flow.list.formEnabled')">
          <el-switch v-model="triggerForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveTrigger">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { useAppStore } from '@/store/modules/app'
import { flowApi, versionApi, triggerApi, runApi } from '@/api/flow'
import { projectApi, type ProjectVO } from '@/api/project'
import type { FlowVO, FlowSave, FlowVersionVO, FlowTriggerVO, FlowTriggerSave, TriggerType } from '@/types/flow'

const router = useRouter()
const appStore = useAppStore()
const { t } = useI18n()

const projects = ref<ProjectVO[]>([])
const currentProjectId = ref<number | null>(appStore.currentProjectId) as any
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
const rules = { name: [{ required: true, message: t('flow.list.nameRequired'), trigger: 'blur' }] }

const versionsDrawer = ref(false)
const versions = ref<FlowVersionVO[]>([])
const currentFlow = ref<FlowVO | null>(null)

const triggersDrawer = ref(false)
const triggers = ref<FlowTriggerVO[]>([])
const triggerDialogVisible = ref(false)
const triggerForm = ref<FlowTriggerSave & { id?: number }>({ type: 'manual', enabled: 1, description: '' })
const triggerConfigJson = ref('')

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
    ElMessage.warning(t('flow.list.selectProjectFirst'))
    return
  }
  form.value = { projectId: currentProjectId.value, name: '', description: '', isAssistant: 0 }
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  if (form.value.id) {
    await flowApi.update(form.value)
    ElMessage.success(t('flow.list.updated'))
  } else {
    await flowApi.create(form.value)
    ElMessage.success(t('flow.list.created'))
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
  const { value: changelog } = await ElMessageBox.prompt(t('flow.list.newVersionPlaceholder'), t('flow.list.newVersionTitle'), {
    inputPlaceholder: t('flow.list.newVersionPlaceholder')
  }).catch(() => ({ value: '' }))
  if (changelog === '') return
  await versionApi.create(currentFlow.value.id, {
    design: currentFlow.value.design || { nodes: [], edges: [] },
    changelog
  })
  ElMessage.success(t('flow.list.versionCreated'))
  versions.value = await versionApi.list(currentFlow.value.id)
}

async function onPublish(v: FlowVersionVO) {
  if (!currentFlow.value) return
  await versionApi.publish(currentFlow.value.id, v.id)
  ElMessage.success(t('flow.list.published', { version: v.version }))
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
      ElMessage.error(t('flow.list.configError'))
      return
    }
  }
  await triggerApi.create(currentFlow.value.id, { ...triggerForm.value, config })
  triggerDialogVisible.value = false
  ElMessage.success(t('flow.list.triggerCreated'))
  triggers.value = await triggerApi.list(currentFlow.value.id)
}

async function onDeleteTrigger(row: FlowTriggerVO) {
  await ElMessageBox.confirm(t('flow.list.deleteTriggerConfirm', { type: row.type }), t('common.confirm'), { type: 'warning' })
  if (!currentFlow.value) return
  await triggerApi.remove(currentFlow.value.id, row.id)
  ElMessage.success(t('flow.list.triggerRemoved'))
  triggers.value = await triggerApi.list(currentFlow.value.id)
}

async function onRun(row: FlowVO) {
  const { value: inputJson } = await ElMessageBox.prompt(t('flow.list.runDialogTitle'), t('flow.list.runDialogTitle'), {
    inputPlaceholder: t('flow.list.runInputPlaceholder'),
    inputType: 'textarea'
  }).catch(() => ({ value: '{}' }))
  let input: any
  try {
    input = inputJson ? JSON.parse(inputJson) : {}
  } catch {
    ElMessage.error(t('flow.list.runJsonError'))
    return
  }
  try {
    const run = await runApi.run(row.id, { input })
    ElMessage.success(t('flow.list.runSuccess', { status: run.status, cost: run.costMs ?? 0 }))
  } catch {
    ElMessage.error(t('flow.list.runFailed'))
  }
}

async function onDelete(row: FlowVO) {
  await ElMessageBox.confirm(t('flow.list.deleteFlowConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await flowApi.remove(row.id)
  ElMessage.success(t('common.delete'))
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
