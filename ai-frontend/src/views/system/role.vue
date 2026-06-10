<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.role.title') }}</span>
      <el-button v-if="can('system:role:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('system.role.add') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('system.role.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('system.role.colName')" prop="name" width="160" />
      <el-table-column :label="t('system.role.colCode')" prop="code" width="160" />
      <el-table-column :label="t('system.role.colDesc')" prop="description" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('system.role.colSort')" prop="sortOrder" width="80" />
      <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:role:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('system:role:delete') && row.code !== 'admin'" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? t('system.role.editTitle') : t('system.role.addTitle')" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('system.role.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('system.role.formCode')" prop="code"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item :label="t('system.role.formDesc')"><el-input v-model="form.description" /></el-form-item>
        <el-form-item :label="t('system.role.formSort')"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item :label="t('system.role.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('system.role.formPermissions')">
          <el-input v-model="permText" type="textarea" :rows="3" :placeholder="t('system.role.formPermissionsPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { roleApi, type RoleVO, type RoleSave } from '@/api/system/role'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<RoleVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleSave>({ name: '', code: '', description: '', sortOrder: 0, status: 1, permissions: [] })
const permText = ref('')
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('system.role.nameRequired'), trigger: 'blur' }],
  code: [{ required: true, message: t('system.role.codeRequired'), trigger: 'blur' }]
}))

async function reload() {
  loading.value = true
  try {
    const res = await roleApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { id: undefined, name: '', code: '', description: '', sortOrder: 0, status: 1, permissions: [] })
  permText.value = ''
  dialogVisible.value = true
}

async function onEdit(row: RoleVO) {
  const r = await roleApi.get(row.id)
  Object.assign(form, r)
  permText.value = (r.permissions || []).join(',')
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  form.permissions = permText.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
  if (form.id) await roleApi.update(form)
  else await roleApi.create(form)
  ElMessage.success(t('system.role.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: RoleVO) {
  await ElMessageBox.confirm(t('system.role.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success(t('system.role.removed'))
  reload()
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
