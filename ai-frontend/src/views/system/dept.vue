<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">部门管理</span>
      <el-button v-if="can('system:dept:add')" type="primary" :icon="Plus" @click="onAdd(0)">新增根部门</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column prop="name" label="部门名称" min-width="200" />
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="leader" label="负责人" width="120" />
      <el-table-column prop="phone" label="电话" width="160" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:dept:add')" size="small" @click="onAdd(row.id)">新增子部门</el-button>
          <el-button v-if="can('system:dept:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('system:dept:delete')" size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父部门">
          <el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'name', value: 'id' }" check-strictly clearable style="width: 100%" />
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import { deptApi, type DeptVO } from '@/api/system/dept'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<any[]>([])
const parentOptions = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DeptVO & { id?: number }>({ id: undefined as any, parentId: 0, name: '', code: '', leader: '', phone: '', email: '', sortOrder: 0, status: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

async function reload() {
  loading.value = true
  try {
    rows.value = await deptApi.tree()
    parentOptions.value = [{ id: 0, name: '根部门', children: rows.value }]
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
    await deptApi.create(payload)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: DeptVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]?`, '确认', { type: 'warning' })
  await deptApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>
