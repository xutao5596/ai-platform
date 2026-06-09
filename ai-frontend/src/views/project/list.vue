<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">我的项目</span>
      <el-button v-if="can('project:create')" type="primary" :icon="Plus" @click="onAdd">新建项目</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="项目名 / 编码" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="name" label="项目名" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/project/${row.id}`)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.roleCode" :type="roleTagType(row.roleCode)">{{ roleLabel(row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="memberCount" label="成员数" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/project/${row.id}`)">详情</el-button>
          <el-button size="small" @click="$router.push(`/project/${row.id}/members`)">成员</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建项目" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { projectApi, type ProjectVO, type ProjectSave } from '@/api/project'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<ProjectVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ProjectSave>({ name: '', code: '', description: '' })
const rules: FormRules = { name: [{ required: true, message: '请输入项目名', trigger: 'blur' }] }

async function reload() {
  loading.value = true
  try {
    const res = await projectApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { name: '', code: '', description: '' })
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await projectApi.create(form)
  ElMessage.success('已创建')
  dialogVisible.value = false
  reload()
}

const roleLabel = (c: string) => ({ owner: '所有者', admin: '管理员', developer: '开发者', viewer: '观察者' }[c] || c)
const roleTagType = (c: string) => ({ owner: 'danger', admin: 'warning', developer: 'success', viewer: 'info' }[c] || '')

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
