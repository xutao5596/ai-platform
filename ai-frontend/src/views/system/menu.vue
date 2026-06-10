<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.menu.title') }}</span>
      <el-button v-if="can('system:menu:add')" type="primary" :icon="Plus" @click="onAdd()">{{ t('system.menu.addRoot') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      :tree-props="{ children: 'children' }"
      :default-expand-all="true"
      border
    >
      <el-table-column :label="t('system.menu.colTitle')" prop="title" width="180" />
      <el-table-column :label="t('system.menu.colName')" prop="name" width="160" />
      <el-table-column :label="t('system.menu.colPath')" prop="path" width="200" />
      <el-table-column :label="t('system.menu.colComponent')" prop="component" width="200" />
      <el-table-column :label="t('system.menu.colIcon')" prop="icon" width="120" />
      <el-table-column :label="t('system.menu.colPermCode')" prop="permCode" width="200" />
      <el-table-column :label="t('system.menu.colSort')" prop="sortOrder" width="80" />
      <el-table-column :label="t('common.action')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:menu:add')" size="small" @click="onAdd(row.id)">{{ t('system.menu.actionAddChild') }}</el-button>
          <el-button v-if="can('system:menu:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('system:menu:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('system.menu.editTitle') : t('system.menu.addTitle')" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('system.menu.formParent')">
          <el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'title', value: 'id' }" check-strictly clearable style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('system.menu.formTitle')" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item :label="t('system.menu.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('system.menu.formPath')"><el-input v-model="form.path" /></el-form-item>
        <el-form-item :label="t('system.menu.formComponent')"><el-input v-model="form.component" /></el-form-item>
        <el-form-item :label="t('system.menu.formIcon')"><el-input v-model="form.icon" :placeholder="t('system.menu.formIconPlaceholder')" /></el-form-item>
        <el-form-item :label="t('system.menu.formPermCode')"><el-input v-model="form.permCode" /></el-form-item>
        <el-form-item :label="t('system.menu.formType')">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">{{ t('system.menu.typeDir') }}</el-radio>
            <el-radio :value="2">{{ t('system.menu.typeMenu') }}</el-radio>
            <el-radio :value="3">{{ t('system.menu.typeButton') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.menu.formSort')"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item :label="t('common.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { menuApi, type MenuNode, type MenuSave } from '@/api/system/menu'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<MenuNode[]>([])
const parentOptions = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuSave>({ parentId: 0, name: '', title: '', type: 1, sortOrder: 0, status: 1 })
const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: t('system.menu.titleRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('system.menu.nameRequired'), trigger: 'blur' }]
}))

async function reload() {
  loading.value = true
  try {
    rows.value = await menuApi.tree()
    parentOptions.value = [{ id: 0, title: t('system.menu.rootMenu'), children: rows.value }]
  } finally {
    loading.value = false
  }
}

function onAdd(parentId: number = 0) {
  Object.assign(form, { id: undefined, parentId, name: '', title: '', path: '', component: '', icon: '', permCode: '', type: parentId === 0 ? 1 : 2, sortOrder: 0, status: 1, visible: 1 })
  dialogVisible.value = true
}

function onEdit(row: MenuNode) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) await menuApi.update(form)
  else await menuApi.create(form)
  ElMessage.success(t('system.menu.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: MenuNode) {
  await ElMessageBox.confirm(t('system.menu.removeConfirm', { name: row.title }), t('common.confirm'), { type: 'warning' })
  await menuApi.remove(row.id)
  ElMessage.success(t('system.menu.removed'))
  reload()
}

onMounted(reload)
</script>
