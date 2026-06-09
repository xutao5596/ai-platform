<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">我的项目</div>
          <div class="stat-value">{{ stats.projectCount }}</div>
          <el-icon class="stat-icon" style="color:#409eff;"><Folder /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">流程数</div>
          <div class="stat-value">0</div>
          <el-icon class="stat-icon" style="color:#10b981;"><Connection /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">知识库</div>
          <div class="stat-value">0</div>
          <el-icon class="stat-icon" style="color:#f59e0b;"><Reading /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">AI 对话(今日)</div>
          <div class="stat-value">0</div>
          <el-icon class="stat-icon" style="color:#8b5cf6;"><ChatDotRound /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col :span="16">
        <el-card title="欢迎使用 AI Platform">
          <template #header>
            <div class="card-title">快速开始</div>
          </template>
          <el-steps :active="2" align-center>
            <el-step title="创建项目" description="以项目为单位组织资源" />
            <el-step title="配置知识库" description="上传文档,智能检索" />
            <el-step title="拖拽编排流程" description="12 个 AI 节点,5 种触发器" />
            <el-step title="上线运行" description="API Key / Webhook / 监控" />
          </el-steps>
          <el-divider />
          <p class="welcome">
            当前为 <el-tag>Sprint 1 基础就绪</el-tag> 阶段,后续 Sprint 将逐步上线 AI 对话、流程编辑器、AI 助手等核心能力。
          </p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><div class="card-title">个人信息</div></template>
          <div class="profile">
            <el-avatar :size="56">{{ avatarText }}</el-avatar>
            <div class="profile-info">
              <div class="profile-name">{{ userStore.realName }}</div>
              <div class="profile-meta text-muted">
                {{ userStore.userInfo?.deptName || '未分配部门' }}
              </div>
              <div class="profile-roles">
                <el-tag v-for="r in userStore.roles" :key="r" type="info" size="small" class="mr">
                  {{ r }}
                </el-tag>
                <el-tag v-if="userStore.isAdmin" type="success" size="small" class="mr">超管</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { projectApi } from '@/api/project'

const userStore = useUserStore()
const stats = ref({ projectCount: 0 })

const avatarText = computed(() => {
  const n = userStore.userInfo?.realName || userStore.userInfo?.username || '?'
  return n.charAt(0).toUpperCase()
})

onMounted(async () => {
  try {
    const list = await projectApi.mine()
    stats.value.projectCount = list.length
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }
.mt { margin-top: 0; }
.stat-card {
  position: relative;
  overflow: hidden;
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
.card-title { font-weight: 600; }
.welcome { color: var(--ai-text-secondary); line-height: 1.6; }
.profile { display: flex; gap: 16px; align-items: center; }
.profile-info { display: flex; flex-direction: column; gap: 4px; }
.profile-name { font-size: 16px; font-weight: 600; }
.profile-roles { display: flex; gap: 4px; flex-wrap: wrap; }
.mr { margin-right: 4px; }
</style>
