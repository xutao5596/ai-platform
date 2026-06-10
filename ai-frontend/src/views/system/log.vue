<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.log.title') }}</span>
      <el-button type="danger" :icon="Delete" @click="onClear">{{ t('system.log.clear') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('system.log.searchUsername')" clearable @keyup.enter="reload" />
      <el-input v-model="query.module" :placeholder="t('system.log.searchModule')" clearable />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="80" />
      <el-table-column :label="t('system.log.colModule')" prop="module" width="120" />
      <el-table-column :label="t('system.log.colAction')" prop="action" width="140" />
      <el-table-column :label="t('system.log.colMethod')" prop="requestMethod" width="80" />
      <el-table-column :label="t('system.log.colUrl')" prop="requestUrl" />
      <el-table-column :label="t('system.log.colUser')" prop="username" width="120" />
      <el-table-column :label="t('system.log.colIp')" prop="ip" width="140" />
      <el-table-column :label="t('system.log.colCost')" prop="costMs" width="100" />
      <el-table-column :label="t('common.status')" width="80">

        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? t('system.log.statusSuccess') : t('system.log.statusFailed') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('system.log.colTime')" prop="createTime" width="180" />

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
import { useI18n } from 'vue-i18n'
import { Delete } from '@element-plus/icons-vue'
import { logApi, type LogVO } from '@/api/system/log'

const { t } = useI18n()

const loading = ref(false)
const rows = ref<LogVO[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | null>(null)
const query = reactive<{
  current: number
  size: number
  keyword: string
  module: string
  username: string
  status: number | undefined
  startTime?: string
  endTime?: string
}>({
  current: 1,
  size: 20,
  keyword: '',
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
  await ElMessageBox.confirm(t('system.log.clearConfirm'), t('common.confirm'), { type: 'warning' })

  await logApi.clear()
  ElMessage.success(t('system.log.cleared'))
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
