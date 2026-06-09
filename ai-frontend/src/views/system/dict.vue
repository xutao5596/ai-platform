<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">字典管理</span>
      <el-button v-if="can('system:dict:add')" type="primary" :icon="Plus" @click="onAddDict">新增字典</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="类型编码 / 名称" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-click="onSelect">
      <el-table-column prop="typeCode" label="类型编码" width="180" />
      <el-table-column prop="typeName" label="类型名称" width="180" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:dict:edit')" size="small" @click.stop="onEditDict(row)">编辑</el-button>
          <el-button v-if="can('system:dict:add')" size="small" type="primary" @click.stop="onAddItem(row)">新增项</el-button>
          <el-button v-if="can('system:dict:delete')" size="small" type="danger" @click.stop="onDeleteDict(row)">删除</el-button>
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
          <span>字典项 — {{ selected.typeName }} ({{ selected.typeCode }})</span>
          <el-button size="small" @click="selected = null">关闭</el-button>
        </div>
      </template>
      <el-table :data="items" border>
        <el-table-column prop="itemKey" label="Key" width="160" />
        <el-table-column prop="itemValue" label="Value" width="160" />
        <el-table-column prop="label" label="展示标签" width="160" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="onEditItem(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="onDeleteItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dictDialog" :title="dictForm.id ? '编辑字典' : '新增字典'" width="500px">
      <el-form :model="dictForm" label-width="100px">
        <el-form-item label="类型编码" required>
          <el-input v-model="dictForm.typeCode" :disabled="!!dictForm.id" />
        </el-form-item>
        <el-form-item label="类型名称" required>
          <el-input v-model="dictForm.typeName" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="dictForm.description" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dictForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialog = false">取消</el-button>
        <el-button type="primary" @click="onSaveDict">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialog" :title="itemForm.id ? '编辑字典项' : '新增字典项'" width="500px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="类型编码"><el-input v-model="itemForm.typeCode" disabled /></el-form-item>
        <el-form-item label="Key" required><el-input v-model="itemForm.itemKey" /></el-form-item>
        <el-form-item label="Value" required><el-input v-model="itemForm.itemValue" /></el-form-item>
        <el-form-item label="展示标签"><el-input v-model="itemForm.label" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">取消</el-button>
        <el-button type="primary" @click="onSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { dictApi, type DictVO, type DictItemVO } from '@/api/system/dict'
import { useUserStore } from '@/store/modules/user'

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
    await dictApi.createDict(payload)
  }
  ElMessage.success('已保存')
  dictDialog.value = false
  reload()
}

async function onDeleteDict(row: DictVO) {
  await ElMessageBox.confirm(`确定删除字典 [${row.typeName}]?该项下所有字典项也会删除`, '确认', { type: 'warning' })
  await dictApi.removeDict(row.id)
  ElMessage.success('已删除')
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
    await dictApi.createItem(payload)
  }
  ElMessage.success('已保存')
  itemDialog.value = false
  if (selected.value) {
    items.value = await dictApi.items(selected.value.typeCode)
  }
}

async function onDeleteItem(row: DictItemVO) {
  await ElMessageBox.confirm(`确定删除字典项 [${row.itemKey}]?`, '确认', { type: 'warning' })
  await dictApi.removeItem(row.id)
  ElMessage.success('已删除')
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
