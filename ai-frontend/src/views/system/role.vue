<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">角色管理</span>
      <el-button v-if="can('system:role:add')" type="primary" :icon="Plus" @click="onAdd">新增角色</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="名称 / 编码" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:role:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('system:role:delete') && row.code !== 'admin'" size="small" type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="权限">
          <el-input v-model="permText" type="textarea" :rows="3" placeholder="多个权限用逗号分隔,如 system:user:add,system:user:edit" />
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
import { roleApi, type RoleVO, type RoleSave } from '@/api/system/role'
import { useUserStore } from '@/store/modules/user'

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
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

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
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: RoleVO) {
  await ElMessageBox.confirm(`确定删除角色 [${row.name}]?`, '确认', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
