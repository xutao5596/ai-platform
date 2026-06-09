<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.push(`/flow/${flowId}`)">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="page-title">运行历史</span>
      <div style="margin-left: auto">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发器" width="100" />
      <el-table-column prop="costMs" label="耗时 (ms)" width="100" />
      <el-table-column prop="startedAt" label="开始时间" width="180" />
      <el-table-column prop="finishedAt" label="结束时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onShowDetail(row as FlowRunVO)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="暂无运行记录" />

    <el-drawer v-model="detailDrawer" :title="`运行详情 #${currentRun?.id}`" direction="rtl" size="60%">
      <div v-if="currentRun" class="run-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(currentRun.status)" size="small">{{ statusLabel(currentRun.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentRun.costMs ?? 0 }} ms</el-descriptions-item>
          <el-descriptions-item label="触发器">{{ currentRun.triggerType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ currentRun.startedAt }}</el-descriptions-item>
          <el-descriptions-item label="结束时间" :span="2">{{ currentRun.finishedAt }}</el-descriptions-item>
          <el-descriptions-item label="输入" :span="2">
            <pre class="json-block">{{ formatJson(currentRun.input) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="输出" :span="2">
            <pre class="json-block">{{ formatJson(currentRun.output) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRun.errorMsg" label="错误" :span="2">
            <pre class="json-block error">{{ currentRun.errorMsg }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top: 16px">执行步骤</h3>
        <el-table :data="run.steps" border size="small">
          <el-table-column prop="seq" label="#" width="50" />
          <el-table-column prop="nodeName" label="节点" />
          <el-table-column prop="nodeType" label="类型" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMs" label="耗时 (ms)" width="100" />
          <el-table-column prop="startedAt" label="开始时间" width="180" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { runApi } from '@/api/flow'
import type { FlowRunVO, FlowRunStepVO, RunStatus } from '@/types/flow'

const route = useRoute()
const flowId = Number(route.params.id)
const loading = ref(false)
const rows = ref<FlowRunVO[]>([])
const detailDrawer = ref(false)
const currentRun = ref<FlowRunVO | null>(null)
const run = ref<{ run: FlowRunVO; steps: FlowRunStepVO[] }>({ run: {} as FlowRunVO, steps: [] })

function statusLabel(s: RunStatus) {
  return ({ pending: '待运行', running: '运行中', success: '成功', failed: '失败', cancelled: '已取消' } as const)[s] || s
}
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
