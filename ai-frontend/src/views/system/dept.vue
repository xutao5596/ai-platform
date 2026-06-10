<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.dept.title') }}</span>
      <el-button v-if="can('system:dept:add')" type="primary" :icon="Plus" @click="onAdd(0)">{{ t('system.dept.addRoot') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column :label="t('system.dept.colName')" prop="name" min-width="200" />
      <el-table-column :label="t('system.dept.colCode')" prop="code" width="160" />
      <el-table-column :label="t('system.dept.colLeader')" prop="leader" width="120" />
      <el-table-column :label="t('system.dept.colPhone')" prop="phone" width="160" />
      <el-table-column :label="t('system.dept.colEmail')" prop="email" width="200" />
      <el-table-column :label="t('system.dept.colSort')" prop="sortOrder" width="80" />
      <el-table-column :label="t('common.action')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:dept:add')" size="small" @click="onAdd(row.id)">{{ t('system.dept.actionAddChild') }}</el-button>
          <el-button v-if="can('system:dept:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('system:dept:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('system.dept.editTitle') : t('system.dept.addTitle')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('system.dept.formParent')">
          <el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'name', value: 'id' }" check-strictly clearable style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('system.dept.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('system.dept.formCode')"><el-input v-model="form.code" /></el-form-item>
        <el-form-item :label="t('system.dept.formLeader')"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item :label="t('system.dept.formPhone')"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item :label="t('system.dept.formEmail')"><el-input v-model="form.email" /></el-form-item>
        <el-form-item :label="t('system.dept.formSort')"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import { deptApi, type DeptVO } from '@/api/system/dept'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<any[]>([])
const parentOptions = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DeptVO & { id?: number }>({ id: undefined as any, parentId: 0, name: '', code: '', leader: '', phone: '', email: '', sortOrder: 0, status: 1 })
const rules = computed<FormRules>(() => ({ name: [{ required: true, message: t('system.dept.nameRequired'), trigger: 'blur' }] }))

async function reload() {
  loading.value = true
  try {
    rows.value = await deptApi.tree()
    parentOptions.value = [{ id: 0, name: t('system.dept.rootDept'), children: rows.value }]
  } finally {
    loading.value = false
  }
}

function onAdd(parentId: number = 0) {
  Object.assign(form, { id: undefined, parentId, name: '', code: '', leader: '', phone: '', email: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

function onEdit(row: DeptVO) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) await deptApi.update(form)
  else {
    const { id, ...payload } = form
    await deptApi.create(payload as any)
  }
  ElMessage.success(t('system.dept.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: DeptVO) {
  await ElMessageBox.confirm(t('system.dept.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await deptApi.remove(row.id)
  ElMessage.success(t('system.dept.removed'))
  reload()
}

onMounted(reload)
</script>
