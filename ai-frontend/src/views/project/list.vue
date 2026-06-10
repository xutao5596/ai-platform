<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('project.list.title') }}</span>
      <el-button v-if="can('project:create')" type="primary" :icon="Plus" @click="onAdd">{{ t('project.list.add') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('project.list.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('project.list.colName')" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/project/${row.id}`)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.list.colCode')" prop="code" width="160" />
      <el-table-column :label="t('project.list.colDesc')" prop="description" />
      <el-table-column :label="t('project.list.colRole')" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.roleCode" :type="roleTagType(row.roleCode) as any">{{ t('project.role.' + row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.list.colMemberCount')" prop="memberCount" width="100" />
      <el-table-column :label="t('project.list.colCreateTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/project/${row.id}`)">{{ t('project.list.actionDetail') }}</el-button>
          <el-button size="small" @click="$router.push(`/project/${row.id}/members`)">{{ t('project.list.actionMembers') }}</el-button>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="t('project.list.addDialogTitle')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('project.list.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('project.list.formCode')"><el-input v-model="form.code" /></el-form-item>
        <el-form-item :label="t('project.list.formDesc')"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { projectApi, type ProjectVO, type ProjectSave } from '@/api/project'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<ProjectVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ProjectSave>({ name: '', code: '', description: '' })
const rules = computed<FormRules>(() => ({ name: [{ required: true, message: t('project.list.nameRequired'), trigger: 'blur' }] }))

const roleTagType = (c: string) => ({ owner: 'danger', admin: 'warning', developer: 'success', viewer: 'info' }[c] || '')

async function reload() {
  loading.value = true
  try {
    const res = await projectApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { name: '', code: '', description: '' })
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await projectApi.create(form)
  ElMessage.success(t('project.list.created'))
  dialogVisible.value = false
  reload()
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
