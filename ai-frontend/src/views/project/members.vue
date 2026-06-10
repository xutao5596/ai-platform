<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> {{ t('project.members.back') }}
      </el-button>
      <span class="page-title">{{ t('project.members.title') }}</span>
      <div class="flex-spacer" />
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="onAdd">{{ t('project.members.add') }}</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column :label="t('project.members.colUsername')" prop="username" width="140" />
      <el-table-column :label="t('project.members.colRealName')" prop="realName" width="140" />
      <el-table-column :label="t('project.members.colRole')" width="160">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.roleCode) as any">{{ t('project.role.' + row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.members.colJoinTime')" prop="joinTime" width="200" />
      <el-table-column v-if="canManage" :label="t('common.action')" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onChangeRole(row)">{{ t('project.members.actionChangeRole') }}</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">{{ t('project.members.actionRemove') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addVisible" :title="t('project.members.addDialogTitle')" width="500px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item :label="t('project.members.formUsername')">
          <el-input v-model="addForm.username" :placeholder="t('project.members.formUsernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('project.members.formRole')">
          <el-select v-model="addForm.roleCode" style="width: 100%">
            <el-option value="admin" :label="t('project.members.roleAdmin')" />
            <el-option value="developer" :label="t('project.members.roleDeveloper')" />
            <el-option value="viewer" :label="t('project.members.roleViewer')" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveAdd">{{ t('project.members.addBtn') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { projectApi, type ProjectMember } from '@/api/project'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const { t } = useI18n()
const id = Number(route.params.id)
const userStore = useUserStore()
const canManage = computed(() => userStore.isAdmin || userStore.userInfo?.id === undefined)

const loading = ref(false)
const rows = ref<ProjectMember[]>([])

const addVisible = ref(false)
const addForm = reactive({ username: '', roleCode: 'developer' })

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
    ElMessage.warning(t('project.members.usernameRequired'))
    return
  }
  await projectApi.addMember(id, addForm)
  ElMessage.success(t('project.members.added'))
  addVisible.value = false
  load()
}

async function onChangeRole(row: ProjectMember) {
  const { value } = await ElMessageBox.prompt(t('project.members.changeRoleDialog'), t('project.members.changeRoleDialog'), {
    inputValue: row.roleCode,
    inputType: 'select',
    inputOptions: [
      { value: 'admin', label: t('project.members.roleAdmin') },
      { value: 'developer', label: t('project.members.roleDeveloper') },
      { value: 'viewer', label: t('project.members.roleViewer') }
    ]
  } as any).catch(() => ({ value: null } as any))
  if (!value) return
  await projectApi.updateMemberRole(id, row.userId, value as string)
  ElMessage.success(t('project.members.updated'))
  load()
}

async function onRemove(row: ProjectMember) {
  await ElMessageBox.confirm(t('project.members.removeConfirm', { name: row.username }), t('common.confirm'), { type: 'warning' })
  await projectApi.removeMember(id, row.userId)
  ElMessage.success(t('project.members.removed'))
  load()
}

watch(() => route.params.id, load, { immediate: true })
onMounted(load)
</script>
