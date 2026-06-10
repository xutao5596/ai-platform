<template>
  <div v-loading="loading" class="page-container">
    <div class="detail-header">
      <el-button text @click="$router.push('/project')">
        <el-icon><ArrowLeft /></el-icon> {{ t('project.detail.back') }}
      </el-button>
      <h2 class="title">{{ project?.name || t('project.detail.defaultTitle') }}</h2>
      <div class="flex-spacer" />
      <el-button @click="$router.push(`/project/${id}/members`)">{{ t('project.detail.membersBtn') }}</el-button>
      <el-button @click="$router.push(`/project/${id}/apikeys`)">{{ t('project.detail.apiKeysBtn') }}</el-button>
      <el-button @click="$router.push(`/project/${id}/webhooks`)">{{ t('project.detail.webhooksBtn') }}</el-button>

    </div>

    <el-tabs v-model="active" class="mt">
      <el-tab-pane :label="t('project.detail.tabOverview')" name="overview">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('project.detail.labelName')">{{ project?.name }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelCode')">{{ project?.code }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelDesc')" :span="2">{{ project?.description || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelRole')">
            <el-tag v-if="project?.roleCode" :type="roleTagType(project.roleCode) as any">{{ t('project.role.' + project.roleCode) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelMemberCount')">{{ project?.memberCount }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelCreateTime')">{{ project?.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabFlow')" name="flow">
        <el-empty :description="t('project.detail.hintFlow')" />
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabKnowledge')" name="knowledge">
        <el-empty :description="t('project.detail.hintKb')" />
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabAssistant')" name="assistant">
        <el-empty :description="t('project.detail.hintAssistant')" />
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabSettings')" name="settings">
        <el-empty :description="t('project.detail.hintSettings')" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'
import { projectApi, type ProjectVO } from '@/api/project'

const route = useRoute()
const { t } = useI18n()
const id = Number(route.params.id)
const project = ref<ProjectVO | null>(null)
const loading = ref(false)
const active = ref('overview')

const roleTagType = (c: string) => ({ owner: 'danger', admin: 'warning', developer: 'success', viewer: 'info' }[c] || '')

async function load() {
  loading.value = true
  try {
    project.value = await projectApi.get(id)
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
onMounted(load)
</script>

<style scoped>
.detail-header { display: flex; align-items: center; gap: 12px; }
.title { margin: 0; font-size: 20px; color: var(--ai-text); }
.mt { margin-top: 16px; }
.flex-spacer { flex: 1; }
</style>
