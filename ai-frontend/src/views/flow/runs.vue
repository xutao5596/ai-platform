<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.push(`/flow/${flowId}`)">
        <el-icon><ArrowLeft /></el-icon> {{ t('flow.runs.back') }}
      </el-button>
      <span class="page-title">{{ t('flow.runs.title') }}</span>
      <div style="margin-left: auto">
        <el-button :icon="Refresh" @click="load">{{ t('flow.runs.refresh') }}</el-button>
      </div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('flow.runs.colStatus')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ t('flowStatus.' + row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('flow.runs.colTrigger')" prop="triggerType" width="100" />
      <el-table-column :label="t('flow.runs.colCost')" prop="costMs" width="100" />
      <el-table-column :label="t('flow.runs.colStartedAt')" prop="startedAt" width="180" />
      <el-table-column :label="t('flow.runs.colFinishedAt')" prop="finishedAt" width="180" />
      <el-table-column :label="t('common.action')" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onShowDetail(row as FlowRunVO)">{{ t('flow.runs.actionDetail') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" :description="t('flow.runs.empty')" />

    <el-drawer v-model="detailDrawer" :title="t('flow.runs.detailTitle', { id: currentRun?.id })" direction="rtl" size="60%">
      <div v-if="currentRun" class="run-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('flow.runs.colStatus')">
            <el-tag :type="statusTag(currentRun.status)" size="small">{{ t('flowStatus.' + currentRun.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelCost')">{{ currentRun.costMs ?? 0 }} ms</el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelTrigger')">{{ currentRun.triggerType || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelStartedAt')">{{ currentRun.startedAt }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelFinishedAt')" :span="2">{{ currentRun.finishedAt }}</el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelInput')" :span="2">
            <pre class="json-block">{{ formatJson(currentRun.input) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item :label="t('flow.runs.labelOutput')" :span="2">
            <pre class="json-block">{{ formatJson(currentRun.output) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRun.errorMsg" :label="t('flow.runs.labelError')" :span="2">
            <pre class="json-block error">{{ currentRun.errorMsg }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top: 16px">{{ t('flow.runs.stepsTitle') }}</h3>
        <el-table :data="run.steps" border size="small">
          <el-table-column :label="t('flow.runs.stepSeq')" prop="seq" width="50" />
          <el-table-column :label="t('flow.runs.stepNode')" prop="nodeName" />
          <el-table-column :label="t('flow.runs.stepType')" prop="nodeType" width="100" />
          <el-table-column :label="t('flow.runs.stepStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ t('flowStatus.' + row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('flow.runs.stepCost')" prop="costMs" width="100" />
          <el-table-column :label="t('flow.runs.stepStartedAt')" prop="startedAt" width="180" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { runApi } from '@/api/flow'
import type { FlowRunVO, FlowRunStepVO, RunStatus } from '@/types/flow'

const route = useRoute()
const { t } = useI18n()
const flowId = Number(route.params.id)
const loading = ref(false)
const rows = ref<FlowRunVO[]>([])
const detailDrawer = ref(false)
const currentRun = ref<FlowRunVO | null>(null)
const run = ref<{ run: FlowRunVO; steps: FlowRunStepVO[] }>({ run: {} as FlowRunVO, steps: [] })

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
    rows.value = await runApi.runs(flowId)
  } finally {
    loading.value = false
  }
}

async function onShowDetail(r: FlowRunVO) {
  currentRun.value = r
  run.value = await runApi.get(r.id)
  detailDrawer.value = true
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.json-block {
  background: #f5f7fa; padding: 8px; border-radius: 4px;
  max-height: 240px; overflow: auto; white-space: pre-wrap; word-wrap: break-word;
  font-size: 12px; margin: 0;
}
.json-block.error { color: #f56c6c; }
</style>
