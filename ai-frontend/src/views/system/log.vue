<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">操作日志</span>
      <el-button type="danger" :icon="Delete" @click="onClear">清空</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.module" placeholder="模块 (project / user ...)" clearable style="width: 160px" @keyup.enter="reload" />
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="reload" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option :value="1" label="成功" />
        <el-option :value="0" label="失败" />
      </el-select>
      <el-date-picker
        v-model="timeRange"
        type="datetimerange"
        range-separator="-"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DDTHH:mm:ss"
        style="width: 360px"
      />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column prop="requestMethod" label="方法" width="80" />
      <el-table-column prop="requestUrl" label="URL" show-overflow-tooltip />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="costMs" label="耗时(ms)" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="onDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @current-change="reload"
      @size-change="reload"
    />

    <el-dialog v-model="detailVisible" title="日志详情" width="780px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
            {{ detail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="URL" :span="2">{{ detail.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.username }} ({{ detail.userId }})</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="UA" :span="2">{{ detail.userAgent }}</el-descriptions-item>
        <el-descriptions-item label="时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误" :span="2">
          <span style="color: #f56c6c">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detail?.requestParams" class="block">
        <div class="block-title">请求参数</div>
        <pre class="code">{{ detail.requestParams }}</pre>
      </div>
      <div v-if="detail?.responseData" class="block">
        <div class="block-title">响应数据</div>
        <pre class="code">{{ detail.responseData }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { logApi, type LogVO } from '@/api/system/log'

const loading = ref(false)
const rows = ref<LogVO[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | null>(null)
const query = reactive<{
  current: number
  size: number
  module: string
  username: string
  status: number | undefined
  startTime?: string
  endTime?: string
}>({
  current: 1,
  size: 20,
  module: '',
  username: '',
  status: undefined
})

watch(timeRange, (v) => {
  query.startTime = v?.[0]
  query.endTime = v?.[1]
  query.current = 1
})

const detailVisible = ref(false)
const detail = ref<LogVO | null>(null)

async function reload() {
  loading.value = true
  try {
    const res = await logApi.page({
      current: query.current,
      size: query.size,
      module: query.module || undefined,
      username: query.username || undefined,
      status: query.status,
      startTime: query.startTime,
      endTime: query.endTime
    })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.current = 1
  query.size = 20
  query.module = ''
  query.username = ''
  query.status = undefined
  query.startTime = undefined
  query.endTime = undefined
  timeRange.value = null
  reload()
}

async function onClear() {
  await ElMessageBox.confirm('确定清空所有审计日志?', '确认', { type: 'warning' })
  await logApi.clear()
  ElMessage.success('已清空')
  reload()
}

function onDetail(row: LogVO) {
  detail.value = row
  detailVisible.value = true
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.block { margin-top: 12px; }
.block-title { font-weight: 600; margin-bottom: 6px; color: var(--el-text-color-secondary); }
.code {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 8px;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
