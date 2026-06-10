<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('system.user.title') }}</span>
      <el-button v-if="can('system:user:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('system.user.add') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('system.user.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-select v-model="query.status" :placeholder="t('system.user.filterStatus')" clearable>
        <el-option :value="1" :label="t('common.enabled')" />
        <el-option :value="0" :label="t('common.disabled')" />
      </el-select>
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
      <el-button @click="reset">{{ t('common.reset') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('system.user.colUsername')" prop="username" width="140" />
      <el-table-column :label="t('system.user.colRealName')" prop="realName" width="120" />
      <el-table-column :label="t('system.user.colEmail')" prop="email" width="200" />
      <el-table-column :label="t('system.user.colPhone')" prop="phone" width="140" />
      <el-table-column :label="t('system.user.colDept')" prop="deptName" width="140" />
      <el-table-column :label="t('system.user.colRoles')" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="c in row.roleCodes" :key="c" size="small" class="mr">{{ c }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('system:user:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('system:user:reset-password')" size="small" @click="onResetPwd(row)">{{ t('system.user.actionResetPwd') }}</el-button>
          <el-button v-if="can('system:user:edit')" size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="onToggleStatus(row)">
            {{ row.status === 1 ? t('system.user.actionDisable') : t('system.user.actionEnable') }}
          </el-button>
          <el-button v-if="can('system:user:delete') && !row.admin" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? t('system.user.editTitle') : t('system.user.addTitle')" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('system.user.formUsername')" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" :label="t('system.user.formPassword')" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('system.user.formRealName')" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item :label="t('system.user.formEmail')" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item :label="t('system.user.formPhone')">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item :label="t('system.user.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('system.user.formRoles')">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="`${r.name} (${r.code})`" :value="r.id" />
          </el-select>
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
import { userApi, type UserVO, type UserSave } from '@/api/system/user'
import { roleApi, type RoleVO } from '@/api/system/role'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
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
const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('system.user.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('system.user.passwordRequired'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('system.user.emailFormat'), trigger: 'blur' }]
}))

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
    ElMessage.success(t('system.user.updated'))
  } else {
    await userApi.create(form)
    ElMessage.success(t('system.user.created'))
  }
  dialogVisible.value = false
  reload()
}

async function onResetPwd(row: UserVO) {
  const { value } = await ElMessageBox.prompt(t('system.user.resetPwdTip'), t('system.user.resetPwdTitle'), {
    inputValue: '123456',
    inputValidator: (v) => (v && v.length >= 6) || t('system.user.resetPwdMin')
  })
  await userApi.resetPassword(row.id, value)
  ElMessage.success(t('system.user.resetPwdOk'))
}

async function onToggleStatus(row: UserVO) {
  const next = row.status === 1 ? 0 : 1
  await userApi.updateStatus(row.id, next)
  ElMessage.success(t('system.user.updated'))
  reload()
}

async function onDelete(row: UserVO) {
  await ElMessageBox.confirm(t('system.user.removeConfirm', { name: row.username }), t('common.confirm'), { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success(t('system.user.removed'))
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
