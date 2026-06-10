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
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @current-change="reload"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Delete } from '@element-plus/icons-vue'
import { logApi, type LogVO } from '@/api/system/log'

const { t } = useI18n()

const loading = ref(false)
const rows = ref<LogVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, keyword: '', module: '' })

async function reload() {
  loading.value = true
  try {
    const res = await logApi.page({ current: query.current, size: query.size, username: query.keyword, module: query.module })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function onClear() {
  await ElMessageBox.confirm(t('system.log.clearConfirm'), t('common.confirm'), { type: 'warning' })
  await logApi.clear()
  ElMessage.success(t('system.log.cleared'))
  reload()
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
