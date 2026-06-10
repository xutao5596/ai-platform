<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button text @click="$router.push('/flow')">
          <el-icon><ArrowLeft /></el-icon> {{ t('flow.detail.back') }}
        </el-button>
        <span class="page-title">{{ flow?.name || t('flow.detail.loading') }}</span>
        <el-tag v-if="flow?.isAssistant === 1" type="warning" size="small" style="margin-left: 8px">{{ t('flow.detail.assistantTag') }}</el-tag>
      </div>
      <div>
        <el-button type="primary" @click="$router.push(`/flow/${flowId}/editor`)">
          {{ t('flow.detail.openEditor') }}
        </el-button>
        <el-button @click="$router.push(`/flow/${flowId}/runs`)">{{ t('flow.detail.runsHistory') }}</el-button>
      </div>
    </div>

    <el-tabs v-model="active" class="mt" @tab-change="onTabChange">
      <el-tab-pane :label="t('flow.detail.tabOverview')" name="overview">
        <el-descriptions v-if="flow" :column="2" border>
          <el-descriptions-item :label="t('flow.detail.labelId')">{{ flow.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelProjectId')">{{ flow.projectId }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelName')">{{ flow.name }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelStatus')">
            <el-tag :type="flow.status === 1 ? 'success' : 'info'" size="small">
              {{ flow.status === 1 ? t('flow.detail.statusPublished') : t('flow.detail.statusDraft') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelVersion')">{{ flow.version || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelCreateTime')">{{ flow.createTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.detail.labelDesc')" :span="2">{{ flow.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane :label="t('flow.detail.tabRuns')" name="runs">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('flow.detail.runsListTitle') }}</span>
          <el-button :icon="Refresh" size="small" @click="loadRuns">{{ t('common.refresh') }}</el-button>
        </div>
        <el-table v-loading="runsLoading" :data="runs" border stripe>
          <el-table-column :label="t('common.id')" prop="id" width="60" />
          <el-table-column :label="t('flow.detail.colRunStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ t('flowStatus.' + row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('flow.detail.colRunTrigger')" prop="triggerType" width="100" />
          <el-table-column :label="t('flow.detail.colRunCost')" prop="costMs" width="100">
            <template #default="{ row }">{{ row.costMs ?? 0 }} ms</template>
          </el-table-column>
          <el-table-column :label="t('flow.detail.colRunInput')" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ formatJson(row.input) }}</template>
          </el-table-column>
          <el-table-column :label="t('flow.detail.colRunOutput')" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ formatJson(row.output) }}</template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="onShowRunDetail(row as FlowRunVO)">{{ t('flow.detail.runActionSteps') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!runsLoading && runs.length === 0" :description="t('flow.detail.runsEmpty')" />

        <el-drawer v-model="runDrawer" :title="t('flow.detail.runDetailTitle', { id: currentRun?.id })" direction="rtl" size="60%">
          <div v-if="currentRun" class="run-detail">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('flow.detail.colRunStatus')">
                <el-tag :type="statusTag(currentRun.status)" size="small">{{ t('flowStatus.' + currentRun.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colRunCost')">{{ currentRun.costMs ?? 0 }} ms</el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colRunTrigger')">{{ currentRun.triggerType || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('common.createTime')">{{ currentRun.createTime }}</el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colRunInput')" :span="2">
                <pre class="json-block">{{ formatJson(currentRun.input) }}</pre>
              </el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colRunOutput')" :span="2">
                <pre class="json-block">{{ formatJson(currentRun.output) }}</pre>
              </el-descriptions-item>
              <el-descriptions-item v-if="currentRun.errorMsg" :label="t('flow.detail.colRunError')" :span="2">
                <pre class="json-block error">{{ currentRun.errorMsg }}</pre>
              </el-descriptions-item>
            </el-descriptions>

            <h3 style="margin-top: 16px">{{ t('flow.detail.stepsTitle') }}</h3>
            <el-table :data="runDetail.steps" border size="small">
              <el-table-column :label="t('flow.detail.stepSeq')" prop="seq" width="50" />
              <el-table-column :label="t('flow.detail.stepNode')" prop="nodeName" />
              <el-table-column :label="t('flow.detail.stepType')" prop="nodeType" width="100" />
              <el-table-column :label="t('flow.detail.stepStatus')" width="100">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.status)" size="small">{{ t('flowStatus.' + row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('flow.detail.stepCost')" prop="costMs" width="100" />
            </el-table>
          </div>
        </el-drawer>
      </el-tab-pane>

      <el-tab-pane :label="t('flow.detail.tabTriggers')" name="triggers">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('flow.detail.triggersListTitle') }}</span>
          <el-button type="primary" :icon="Plus" size="small" @click="onAddTrigger">
            {{ t('flow.detail.newTrigger') }}
          </el-button>
        </div>
        <el-table v-loading="triggersLoading" :data="triggers" border stripe>
          <el-table-column :label="t('common.id')" prop="id" width="60" />
          <el-table-column :label="t('flow.detail.colTriggerType')" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ t('flow.list.trigger' + (String(row.type).charAt(0).toUpperCase() + String(row.type).slice(1))) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('flow.detail.colTriggerDesc')" prop="description" min-width="200" show-overflow-tooltip />
          <el-table-column :label="t('flow.detail.colTriggerEnabled')" width="100">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="onToggleTrigger(row as FlowTriggerVO)" />
            </template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="onDeleteTrigger(row as FlowTriggerVO)">{{ t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!triggersLoading && triggers.length === 0" :description="t('flow.detail.triggersEmpty')" />

        <el-dialog v-model="triggerDialogVisible" :title="t('flow.detail.newTriggerTitle')" width="480px">
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
      </el-tab-pane>

      <el-tab-pane :label="t('flow.detail.tabVersions')" name="versions">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('flow.detail.versionsListTitle') }}</span>
          <el-button type="primary" :icon="Plus" size="small" @click="onCreateVersion">
            {{ t('flow.detail.newVersion') }}
          </el-button>
        </div>
        <el-table v-loading="versionsLoading" :data="versions" border stripe>
          <el-table-column :label="t('common.id')" prop="id" width="60" />
          <el-table-column :label="t('flow.detail.colVersionNo')" prop="version" width="100" />
          <el-table-column :label="t('flow.detail.colChangelog')" prop="changelog" min-width="240" show-overflow-tooltip />
          <el-table-column :label="t('flow.detail.colActive')" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="success" size="small">{{ t('flow.detail.activeTag') }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.isActive !== 1" size="small" type="primary" @click="onPublishVersion(row as FlowVersionVO)">
                {{ t('flow.detail.publish') }}
              </el-button>
              <el-button size="small" @click="onCompareVersion(row as FlowVersionVO)">
                {{ t('flow.detail.compare') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!versionsLoading && versions.length === 0" :description="t('flow.detail.versionsEmpty')" />

        <el-drawer v-model="compareDrawer" :title="t('flow.detail.compareTitle', { v: compareSource?.version || '' })" direction="rtl" size="55%">
          <div v-if="compareSource" class="compare-pane">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('flow.detail.colVersionNo')">{{ compareSource.version }}</el-descriptions-item>
              <el-descriptions-item :label="t('common.createTime')">{{ compareSource.createTime }}</el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colChangelog')" :span="2">{{ compareSource.changelog || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('flow.detail.colDesign')" :span="2">
                <pre class="json-block">{{ formatJson(compareSource.design) }}</pre>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-drawer>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Refresh } from '@element-plus/icons-vue'
import { flowApi, runApi, triggerApi, versionApi } from '@/api/flow'
import type { FlowVO, FlowRunVO, FlowRunStepVO, FlowTriggerVO, FlowTriggerSave, FlowVersionVO, RunStatus } from '@/types/flow'

const route = useRoute()
const { t } = useI18n()
const flowId = Number(route.params.id)
const flow = ref<FlowVO | null>(null)
const loading = ref(false)
const active = ref('overview')

function statusTag(s: RunStatus) {
  return ({ pending: 'info', running: 'warning', success: 'success', failed: 'danger', cancelled: 'info' } as const)[s] || 'info'
}
function formatJson(v: any) {
  if (v === null || v === undefined) return '-'
  try { return typeof v === 'string' ? v : JSON.stringify(v, null, 2) }
  catch { return String(v) }
}

async function load() {
  loading.value = true
  try {
    flow.value = await flowApi.get(flowId)
  } finally {
    loading.value = false
  }
}

const runs = ref<FlowRunVO[]>([])
const runsLoading = ref(false)
const runDrawer = ref(false)
const currentRun = ref<FlowRunVO | null>(null)
const runDetail = ref<{ run: FlowRunVO; steps: FlowRunStepVO[] }>({ run: {} as FlowRunVO, steps: [] })

async function loadRuns() {
  runsLoading.value = true
  try {
    const res = await runApi.runs(flowId)
    runs.value = res.records
  } catch {
    runs.value = []
  } finally {
    runsLoading.value = false
  }
}

async function onShowRunDetail(r: FlowRunVO) {
  currentRun.value = r
  try {
    runDetail.value = await runApi.get(r.id)
  } catch {
    runDetail.value = { run: r, steps: [] }
  }
  runDrawer.value = true
}

const triggers = ref<FlowTriggerVO[]>([])
const triggersLoading = ref(false)
const triggerDialogVisible = ref(false)
const triggerForm = ref<FlowTriggerSave>({ type: 'manual', enabled: 1, description: '' })
const triggerConfigJson = ref('')

async function loadTriggers() {
  triggersLoading.value = true
  try {
    triggers.value = await triggerApi.list(flowId)
  } catch {
    triggers.value = []
  } finally {
    triggersLoading.value = false
  }
}

function onAddTrigger() {
  triggerForm.value = { type: 'manual', enabled: 1, description: '' }
  triggerConfigJson.value = ''
  triggerDialogVisible.value = true
}

async function onSaveTrigger() {
  let config: any
  if (triggerConfigJson.value.trim()) {
    try {
      config = JSON.parse(triggerConfigJson.value)
    } catch {
      ElMessage.error(t('flow.list.configError'))
      return
    }
  }
  await triggerApi.create(flowId, { ...triggerForm.value, config })
  triggerDialogVisible.value = false
  ElMessage.success(t('flow.detail.triggerCreated'))
  loadTriggers()
}

async function onToggleTrigger(row: FlowTriggerVO) {
  try {
    await triggerApi.update(flowId, row.id, {
      type: row.type,
      config: row.config,
      enabled: row.enabled,
      description: row.description
    })
  } catch {
    row.enabled = row.enabled === 1 ? 0 : 1
    ElMessage.error(t('flow.detail.triggerUpdateFailed'))
  }
}

async function onDeleteTrigger(row: FlowTriggerVO) {
  await ElMessageBox.confirm(t('flow.list.deleteTriggerConfirm', { type: row.type }), t('common.confirm'), { type: 'warning' })
  await triggerApi.remove(flowId, row.id)
  ElMessage.success(t('flow.detail.triggerRemoved'))
  loadTriggers()
}

const versions = ref<FlowVersionVO[]>([])
const versionsLoading = ref(false)
const compareDrawer = ref(false)
const compareSource = ref<FlowVersionVO | null>(null)

async function loadVersions() {
  versionsLoading.value = true
  try {
    versions.value = await versionApi.list(flowId)
  } catch {
    versions.value = []
  } finally {
    versionsLoading.value = false
  }
}

async function onCreateVersion() {
  if (!flow.value) return
  const { value: changelog } = await ElMessageBox.prompt(
    t('flow.list.newVersionPlaceholder'),
    t('flow.list.newVersionTitle'),
    { inputPlaceholder: t('flow.list.newVersionPlaceholder') }
  ).catch(() => ({ value: '' as string | undefined }))
  if (!changelog) return
  await versionApi.create(flowId, {
    design: flow.value.design || { nodes: [], edges: [] },
    changelog
  })
  ElMessage.success(t('flow.list.versionCreated'))
  loadVersions()
}

async function onPublishVersion(v: FlowVersionVO) {
  await versionApi.publish(flowId, v.id)
  ElMessage.success(t('flow.list.published', { version: v.version }))
  loadVersions()
}

function onCompareVersion(v: FlowVersionVO) {
  compareSource.value = v
  compareDrawer.value = true
}

const loaded = { runs: false, triggers: false, versions: false }
function onTabChange(name: string | number | undefined) {
  if (!name) return
  if (name === 'runs' && !loaded.runs) { loaded.runs = true; loadRuns() }
  else if (name === 'triggers' && !loaded.triggers) { loaded.triggers = true; loadTriggers() }
  else if (name === 'versions' && !loaded.versions) { loaded.versions = true; loadVersions() }
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.mt { margin-top: 0; }
.tab-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.tab-title { font-size: 15px; font-weight: 500; color: var(--ai-text); }
.json-block {
  background: #f5f7fa; padding: 8px; border-radius: 4px;
  max-height: 240px; overflow: auto; white-space: pre-wrap; word-wrap: break-word;
  font-size: 12px; margin: 0;
}
.json-block.error { color: #f56c6c; }
</style>
