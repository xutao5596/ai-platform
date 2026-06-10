<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('dashboard.myProjects') }}</div>
          <div class="stat-value">{{ stats.projectCount }}</div>
          <el-icon class="stat-icon stat-icon-primary"><Folder /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('dashboard.totalFlows') }}</div>
          <div class="stat-value">{{ stats.flowCount }}</div>
          <el-icon class="stat-icon stat-icon-success"><Connection /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('dashboard.knowledgeCount') }}</div>
          <div class="stat-value">{{ stats.knowledgeCount }}</div>
          <el-icon class="stat-icon stat-icon-warning"><Reading /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" :title="t('monitor.chatPending')">
          <div class="stat-label">{{ t('dashboard.chatToday') }}</div>
          <div class="stat-value">{{ stats.chatToday }}</div>
          <el-icon class="stat-icon stat-icon-purple"><ChatDotRound /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col :span="16">
        <el-card :title="t('dashboard.welcomeCard')">
          <template #header>
            <div class="card-title">{{ t('dashboard.quickStart') }}</div>
          </template>
          <el-steps :active="2" align-center>
            <el-step :title="t('dashboard.stepCreateProject')" :description="t('dashboard.stepCreateProjectDesc')" />
            <el-step :title="t('dashboard.stepKb')" :description="t('dashboard.stepKbDesc')" />
            <el-step :title="t('dashboard.stepFlow')" :description="t('dashboard.stepFlowDesc')" />
            <el-step :title="t('dashboard.stepOnline')" :description="t('dashboard.stepOnlineDesc')" />
          </el-steps>
          <el-divider />
          <p class="welcome">
            {{ t('dashboard.sprintTip', { phase: t('dashboard.sprintTag') }) }}
          </p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><div class="card-title">{{ t('dashboard.profile') }}</div></template>
          <div class="profile">
            <el-avatar :size="56">{{ avatarText }}</el-avatar>
            <div class="profile-info">
              <div class="profile-name">{{ userStore.realName }}</div>
              <div class="profile-meta text-muted">
                {{ userStore.userInfo?.deptName || t('dashboard.noDept') }}
              </div>
              <div class="profile-roles">
                <el-tag v-for="r in userStore.roles" :key="r" type="info" size="small" class="mr">
                  {{ r }}
                </el-tag>
                <el-tag v-if="userStore.isAdmin" type="success" size="small" class="mr">{{ t('dashboard.superAdmin') }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { projectApi } from '@/api/project'
import { flowApi } from '@/api/flow'
import { knowledgeApi } from '@/api/ai/knowledge'

const { t } = useI18n()
const userStore = useUserStore()
const stats = reactive({ projectCount: 0, flowCount: 0, knowledgeCount: 0, chatToday: 0 })

const avatarText = computed(() => {
  const n = userStore.userInfo?.realName || userStore.userInfo?.username || '?'
  return n.charAt(0).toUpperCase()
})

onMounted(async () => {
  // Fire 3 real API calls in parallel; chatToday stays 0 until backend lands
  const results = await Promise.allSettled([
    projectApi.mine(),
    flowApi.page({ current: 1, size: 1 }),
    knowledgeApi.list()
  ])
  const [proj, flow, kb] = results
  if (proj.status === 'fulfilled' && Array.isArray(proj.value)) {
    stats.projectCount = proj.value.length
  }
  if (flow.status === 'fulfilled') {
    stats.flowCount = (flow.value as any)?.total || 0
  }
  if (kb.status === 'fulfilled' && Array.isArray(kb.value)) {
    stats.knowledgeCount = kb.value.length
  }
  // chatToday: backend not implemented — see monitor.chatPending
  stats.chatToday = 0
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }
.mt { margin-top: 0; }
.stat-card {
  position: relative;
  overflow: hidden;
  background: var(--ai-bg-elevated);
  color: var(--ai-text);
}
.stat-label { color: var(--ai-text-secondary); font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 600; margin-top: 8px; color: var(--ai-text); }
.stat-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 48px;
  opacity: 0.15;
}
.stat-icon-primary { color: var(--ai-primary); }
.stat-icon-success { color: var(--ai-success); }
.stat-icon-warning { color: var(--ai-warning); }
.stat-icon-purple { color: #8b5cf6; }
.card-title { font-weight: 600; color: var(--ai-text); }
.welcome { color: var(--ai-text-secondary); line-height: 1.6; }
.profile { display: flex; gap: 16px; align-items: center; }
.profile-info { display: flex; flex-direction: column; gap: 4px; }
.profile-name { font-size: 16px; font-weight: 600; color: var(--ai-text); }
.profile-roles { display: flex; gap: 4px; flex-wrap: wrap; }
.mr { margin-right: 4px; }
</style>
