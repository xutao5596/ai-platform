<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">操作日志</span>
      <el-button type="danger" :icon="Delete" @click="onClear">清空</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="用户名" clearable @keyup.enter="reload" />
      <el-input v-model="query.module" placeholder="模块" clearable />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column prop="requestMethod" label="方法" width="80" />
      <el-table-column prop="requestUrl" label="URL" />
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
import { Delete } from '@element-plus/icons-vue'
import { logApi, type LogVO } from '@/api/system/log'

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
  await ElMessageBox.confirm('确定清空所有操作日志?', '确认', { type: 'warning' })
  await logApi.clear()
  ElMessage.success('已清空')
  reload()
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
