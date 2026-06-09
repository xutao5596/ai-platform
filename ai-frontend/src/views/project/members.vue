<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> 返回项目
      </el-button>
      <span class="page-title">成员管理</span>
      <div class="flex-spacer" />
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="onAdd">添加成员</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" width="140" />
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.roleCode)">{{ roleLabel(row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="joinTime" label="加入时间" width="200" />
      <el-table-column v-if="canManage" label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onChangeRole(row)">调整角色</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addVisible" title="添加成员" width="500px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="addForm.username" placeholder="输入系统用户名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.roleCode" style="width: 100%">
            <el-option value="admin" label="管理员" />
            <el-option value="developer" label="开发者" />
            <el-option value="viewer" label="观察者" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { projectApi, type ProjectMember } from '@/api/project'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const id = Number(route.params.id)
const userStore = useUserStore()
const canManage = computed(() => userStore.isAdmin || userStore.userInfo?.id === undefined)

const loading = ref(false)
const rows = ref<ProjectMember[]>([])

const addVisible = ref(false)
const addForm = reactive({ username: '', roleCode: 'developer' })

const roleLabel = (c: string) => ({ owner: '所有者', admin: '管理员', developer: '开发者', viewer: '观察者' }[c] || c)
const roleTagType = (c: string) => ({ owner: 'danger', admin: 'warning', developer: 'success', viewer: 'info' }[c] || '')

async function load() {
  loading.value = true
  try {
    rows.value = await projectApi.members(id)
  } finally {
    loading.value = false
  }
}

function onAdd() {
  addForm.username = ''
  addForm.roleCode = 'developer'
  addVisible.value = true
}

async function onSaveAdd() {
  if (!addForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  await projectApi.addMember(id, addForm)
  ElMessage.success('已添加')
  addVisible.value = false
  load()
}

async function onChangeRole(row: ProjectMember) {
  const { value } = await ElMessageBox.prompt('选择新角色', '调整角色', {
    inputValue: row.roleCode,
    inputOptions: [
      { value: 'admin', label: '管理员' },
      { value: 'developer', label: '开发者' },
      { value: 'viewer', label: '观察者' }
    ]
  }).catch(() => null)
  if (!value) return
  await projectApi.updateMemberRole(id, row.userId, value as string)
  ElMessage.success('已更新')
  load()
}

async function onRemove(row: ProjectMember) {
  await ElMessageBox.confirm(`确定移除 [${row.username}]?`, '确认', { type: 'warning' })
  await projectApi.removeMember(id, row.userId)
  ElMessage.success('已移除')
  load()
}

watch(() => route.params.id, load, { immediate: true })
onMounted(load)
</script>
