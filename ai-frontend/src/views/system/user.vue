<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">用户管理</span>
      <el-button v-if="can('system:user:add')" type="primary" :icon="Plus" @click="onAdd">新增用户</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="用户名 / 姓名 / 邮箱" clearable @keyup.enter="reload" />
      <el-select v-model="query.status" placeholder="状态" clearable>
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="禁用" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="deptName" label="部门" width="140" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="c in row.roleCodes" :key="c" size="small" class="mr">{{ c }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:user:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('system:user:reset-password')" size="small" @click="onResetPwd(row)">重置密码</el-button>
          <el-button v-if="can('system:user:edit')" size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="onToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button v-if="can('system:user:delete') && !row.admin" size="small" type="danger" @click="onDelete(row)">删除</el-button>
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
      @size-change="reload"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="`${r.name} (${r.code})`" :value="r.id" />
          </el-select>
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
import { userApi, type UserVO, type UserSave } from '@/api/system/user'
import { roleApi, type RoleVO } from '@/api/system/role'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<UserVO[]>([])
const total = ref(0)
const roleList = ref<RoleVO[]>([])
const query = reactive({ current: 1, size: 10, keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<UserSave>({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式错误', trigger: 'blur' }]
}

async function reload() {
  loading.value = true
  try {
    const res = await userApi.page({ ...query, status: query.status })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.status = undefined
  query.current = 1
  reload()
}

function onAdd() {
  Object.assign(form, { id: undefined, username: '', password: '', realName: '', email: '', phone: '', status: 1, roleIds: [] })
  dialogVisible.value = true
}

async function onEdit(row: UserVO) {
  const u = await userApi.get(row.id)
  Object.assign(form, {
    id: u.id,
    username: u.username,
    realName: u.realName,
    email: u.email,
    phone: u.phone,
    status: u.status,
    roleIds: u.roleIds || []
  })
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) {
    const payload = { ...form }
    delete (payload as any).password
    await userApi.update(payload)
    ElMessage.success('已更新')
  } else {
    await userApi.create(form)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  reload()
}

async function onResetPwd(row: UserVO) {
  const { value } = await ElMessageBox.prompt('重置密码(默认 123456)', '提示', {
    inputValue: '123456',
    inputValidator: (v) => (v && v.length >= 6) || '至少 6 位'
  })
  await userApi.resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function onToggleStatus(row: UserVO) {
  const next = row.status === 1 ? 0 : 1
  await userApi.updateStatus(row.id, next)
  ElMessage.success('已更新')
  reload()
}

async function onDelete(row: UserVO) {
  await ElMessageBox.confirm(`确定删除用户 [${row.username}]?`, '确认', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

onMounted(async () => {
  roleList.value = await roleApi.list()
  reload()
})
</script>

<style scoped>
.mr { margin-right: 4px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
