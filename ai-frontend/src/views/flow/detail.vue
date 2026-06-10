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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'
import { flowApi } from '@/api/flow'
import type { FlowVO } from '@/types/flow'

const route = useRoute()
const { t } = useI18n()
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
