<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.dict.title') }}</span>
      <el-button v-if="can('system:dict:add')" type="primary" :icon="Plus" @click="onAddDict">{{ t('system.dict.addDict') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('system.dict.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-click="onSelect">
      <el-table-column :label="t('system.dict.colTypeCode')" prop="typeCode" width="180" />
      <el-table-column :label="t('system.dict.colTypeName')" prop="typeName" width="180" />
      <el-table-column :label="t('system.dict.colDesc')" prop="description" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:dict:edit')" size="small" @click.stop="onEditDict(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('system:dict:add')" size="small" type="primary" @click.stop="onAddItem(row)">{{ t('system.dict.actionAddItem') }}</el-button>
          <el-button v-if="can('system:dict:delete')" size="small" type="danger" @click.stop="onDeleteDict(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @current-change="reload"
    />

    <el-card v-if="selected" class="mt">
      <template #header>
        <div class="card-title-row">
          <span>{{ t('system.dict.itemsTitle', { name: selected.typeName, code: selected.typeCode }) }}</span>
          <el-button size="small" @click="selected = null">{{ t('system.dict.closeItems') }}</el-button>
        </div>
      </template>
      <el-table :data="items" border>
        <el-table-column :label="t('system.dict.colKey')" prop="itemKey" width="160" />
        <el-table-column :label="t('system.dict.colValue')" prop="itemValue" width="160" />
        <el-table-column :label="t('system.dict.colLabel')" prop="label" width="160" />
        <el-table-column :label="t('system.dict.colSort')" prop="sortOrder" width="80" />
        <el-table-column :label="t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="onEditItem(row)">{{ t('common.edit') }}</el-button>
            <el-button size="small" type="danger" @click="onDeleteItem(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dictDialog" :title="dictForm.id ? t('system.dict.editDictTitle') : t('system.dict.addDictTitle')" width="500px">
      <el-form :model="dictForm" label-width="100px">
        <el-form-item :label="t('system.dict.formTypeCode')" required>
          <el-input v-model="dictForm.typeCode" :disabled="!!dictForm.id" />
        </el-form-item>
        <el-form-item :label="t('system.dict.formTypeName')" required>
          <el-input v-model="dictForm.typeName" />
        </el-form-item>
        <el-form-item :label="t('system.dict.formDesc')"><el-input v-model="dictForm.description" /></el-form-item>
        <el-form-item :label="t('common.status')">
          <el-switch v-model="dictForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveDict">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialog" :title="itemForm.id ? t('system.dict.editItemTitle') : t('system.dict.addItemTitle')" width="500px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item :label="t('system.dict.itemFormTypeCode')"><el-input v-model="itemForm.typeCode" disabled /></el-form-item>
        <el-form-item :label="t('system.dict.formKey')" required><el-input v-model="itemForm.itemKey" /></el-form-item>
        <el-form-item :label="t('system.dict.formValue')" required><el-input v-model="itemForm.itemValue" /></el-form-item>
        <el-form-item :label="t('system.dict.formLabel')"><el-input v-model="itemForm.label" /></el-form-item>
        <el-form-item :label="t('system.dict.formSort')"><el-input-number v-model="itemForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item :label="t('system.dict.itemFormStatus')">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveItem">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { dictApi, type DictVO, type DictItemVO } from '@/api/system/dict'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<DictVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const selected = ref<DictVO | null>(null)
const items = ref<DictItemVO[]>([])

const dictDialog = ref(false)
const dictForm = reactive<DictVO>({ id: undefined as any, typeCode: '', typeName: '', description: '', status: 1 })

const itemDialog = ref(false)
const itemForm = reactive<DictItemVO>({ id: undefined as any, typeCode: '', itemKey: '', itemValue: '', label: '', sortOrder: 0, status: 1 })

async function reload() {
  loading.value = true
  try {
    const res = await dictApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function onSelect(row: DictVO) {
  selected.value = row
  items.value = await dictApi.items(row.typeCode)
}

function onAddDict() {
  Object.assign(dictForm, { id: undefined, typeCode: '', typeName: '', description: '', status: 1 })
  dictDialog.value = true
}

function onEditDict(row: DictVO) {
  Object.assign(dictForm, row)
  dictDialog.value = true
}

async function onSaveDict() {
  if (dictForm.id) await dictApi.updateDict(dictForm)
  else {
    const { id, ...payload } = dictForm
    await dictApi.createDict(payload as any)
  }
  ElMessage.success(t('system.dict.saved'))
  dictDialog.value = false
  reload()
}

async function onDeleteDict(row: DictVO) {
  await ElMessageBox.confirm(t('system.dict.removeDictConfirm', { name: row.typeName }), t('common.confirm'), { type: 'warning' })
  await dictApi.removeDict(row.id)
  ElMessage.success(t('system.dict.dictRemoved'))
  if (selected.value?.id === row.id) selected.value = null
  reload()
}

function onAddItem(row: DictVO) {
  Object.assign(itemForm, { id: undefined, typeCode: row.typeCode, itemKey: '', itemValue: '', label: '', sortOrder: 0, status: 1 })
  itemDialog.value = true
}

function onEditItem(row: DictItemVO) {
  Object.assign(itemForm, row)
  itemDialog.value = true
}

async function onSaveItem() {
  if (itemForm.id) await dictApi.updateItem(itemForm)
  else {
    const { id, ...payload } = itemForm
    await dictApi.createItem(payload as any)
  }
  ElMessage.success(t('system.dict.saved'))
  itemDialog.value = false
  if (selected.value) {
    items.value = await dictApi.items(selected.value.typeCode)
  }
}

async function onDeleteItem(row: DictItemVO) {
  await ElMessageBox.confirm(t('system.dict.removeItemConfirm', { key: row.itemKey }), t('common.confirm'), { type: 'warning' })
  await dictApi.removeItem(row.id)
  ElMessage.success(t('system.dict.itemRemoved'))
  if (selected.value) {
    items.value = await dictApi.items(selected.value.typeCode)
  }
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.mt { margin-top: 16px; }
.card-title-row { display: flex; justify-content: space-between; align-items: center; }
</style>
