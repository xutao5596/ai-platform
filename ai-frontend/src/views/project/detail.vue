<template>
  <div v-loading="loading" class="page-container">
    <div class="detail-header">
      <el-button text @click="$router.push('/project')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2 class="title">{{ project?.name || '项目详情' }}</h2>
      <div class="flex-spacer" />
      <el-button @click="$router.push(`/project/${id}/members`)">成员管理</el-button>
    </div>

    <el-tabs v-model="active" class="mt">
      <el-tab-pane label="概览" name="overview">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名">{{ project?.name }}</el-descriptions-item>
          <el-descriptions-item label="编码">{{ project?.code }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ project?.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="我的角色">
            <el-tag v-if="project?.roleCode" :type="roleTagType(project.roleCode)">{{ roleLabel(project.roleCode) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="成员数">{{ project?.memberCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ project?.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="流程" name="flow">
        <el-empty description="Sprint 3 上线流程编辑器" />
      </el-tab-pane>

      <el-tab-pane label="知识库" name="knowledge">
        <el-empty description="Sprint 2 上线知识库" />
      </el-tab-pane>

      <el-tab-pane label="助手" name="assistant">
        <el-empty description="Sprint 3 上线助手" />
      </el-tab-pane>

      <el-tab-pane label="设置" name="settings">
        <el-empty description="Sprint 4 完善" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { projectApi, type ProjectVO } from '@/api/project'

const route = useRoute()
const id = Number(route.params.id)
const project = ref<ProjectVO | null>(null)
const loading = ref(false)
const active = ref('overview')

const roleLabel = (c: string) => ({ owner: '所有者', admin: '管理员', developer: '开发者', viewer: '观察者' }[c] || c)
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
.title { margin: 0; font-size: 20px; }
.mt { margin-top: 16px; }
.flex-spacer { flex: 1; }
</style>
