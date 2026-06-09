<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button text @click="$router.push('/flow')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <span class="page-title">{{ flow?.name || '加载中...' }}</span>
        <el-tag v-if="flow?.isAssistant === 1" type="warning" size="small" style="margin-left: 8px">助手</el-tag>
      </div>
      <div>
        <el-button type="primary" @click="$router.push(`/flow/${flowId}/editor`)">
          打开编辑器
        </el-button>
        <el-button @click="$router.push(`/flow/${flowId}/runs`)">运行历史</el-button>
      </div>
    </div>

    <el-descriptions v-if="flow" :column="2" border>
      <el-descriptions-item label="ID">{{ flow.id }}</el-descriptions-item>
      <el-descriptions-item label="项目 ID">{{ flow.projectId }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ flow.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="flow.status === 1 ? 'success' : 'info'" size="small">
          {{ flow.status === 1 ? '已发布' : '草稿' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="版本">{{ flow.version || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ flow.createTime }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ flow.description || '-' }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { flowApi } from '@/api/flow'
import type { FlowVO } from '@/types/flow'

const route = useRoute()
const flowId = Number(route.params.id)
const flow = ref<FlowVO | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    flow.value = await flowApi.get(flowId)
  } finally {
    loading.value = false
  }
})
</script>
