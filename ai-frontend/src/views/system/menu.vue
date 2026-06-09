<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">菜单管理</span>
      <el-button v-if="can('system:menu:add')" type="primary" :icon="Plus" @click="onAdd()">新增根菜单</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      :tree-props="{ children: 'children' }"
      :default-expand-all="true"
      border
      default-expand-all
    >
      <el-table-column prop="title" label="标题" width="180" />
      <el-table-column prop="name" label="路由名" width="160" />
      <el-table-column prop="path" label="路径" width="200" />
      <el-table-column prop="component" label="组件" width="200" />
      <el-table-column prop="icon" label="图标" width="120" />
      <el-table-column prop="permCode" label="权限标识" width="200" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:menu:add')" size="small" @click="onAdd(row.id)">新增子项</el-button>
          <el-button v-if="can('system:menu:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('system:menu:delete')" size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父菜单">
          <el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'title', value: 'id' }" check-strictly clearable style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="路由名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="路径"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="组件"><el-input v-model="form.component" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="如 Folder / Setting" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permCode" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { menuApi, type MenuNode, type MenuSave } from '@/api/system/menu'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<MenuNode[]>([])
const parentOptions = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuSave>({ parentId: 0, name: '', title: '', type: 1, sortOrder: 0, status: 1 })
const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  name: [{ required: true, message: '请输入路由名', trigger: 'blur' }]
}

async function reload() {
  loading.value = true
  try {
    rows.value = await menuApi.tree()
    parentOptions.value = [{ id: 0, title: '根菜单', children: rows.value }]
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
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: MenuNode) {
  await ElMessageBox.confirm(`确定删除 [${row.title}]?`, '确认', { type: 'warning' })
  await menuApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>
