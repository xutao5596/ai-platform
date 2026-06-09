<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">提示词</span>
      <el-button v-if="can('ai:prompt:add')" type="primary" :icon="Plus" @click="onAdd">新建提示词</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="名称 / 编码" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="versionCount" label="版本数" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:prompt:edit')" size="small" @click="onVersions(row)">版本</el-button>
          <el-button v-if="can('ai:prompt:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('ai:prompt:delete')" size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑提示词' : '新建提示词'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="项目 ID" prop="projectId"><el-input-number v-model="form.projectId" :min="1" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionsDrawer" title="版本管理" direction="rtl" size="60%">
      <div v-if="currentPrompt" class="versions-content">
        <div class="versions-header">
          <h3>{{ currentPrompt.name }} ({{ currentPrompt.code }})</h3>
          <el-button v-if="can('ai:prompt:add')" type="primary" :icon="Plus" @click="onNewVersion">新建版本</el-button>
        </div>
        <el-table :data="versions" border>
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="changelog" label="变更" />
          <el-table-column label="当前" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="success" size="small">激活</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.isActive !== 1" size="small" @click="onActivate(row)">设为激活</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="versionDialog" :title="versionForm.id ? '编辑版本' : '新建版本'" width="700px">
      <el-form :model="versionForm" label-width="100px">
        <el-form-item label="内容">
          <el-input v-model="versionForm.content" type="textarea" :rows="10" placeholder="使用 {{var}} 表示变量" />
        </el-form-item>
        <el-form-item label="变更说明"><el-input v-model="versionForm.changelog" /></el-form-item>
        <el-form-item label="设为激活版本"><el-switch v-model="versionForm.activate" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionDialog = false">取消</el-button>
        <el-button type="primary" @click="onSaveVersion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { promptApi, type PromptVO, type PromptSave, type PromptVersionVO, type PromptVersionSave } from '@/api/ai/prompt'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<PromptVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PromptSave>({ projectId: 1, name: '', code: '', description: '', status: 1 })
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '项目 ID 必填', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

const versionsDrawer = ref(false)
const currentPrompt = ref<PromptVO | null>(null)
const versions = ref<PromptVersionVO[]>([])

const versionDialog = ref(false)
const versionForm = reactive<PromptVersionSave>({ promptId: 0, content: '', changelog: '', activate: false })

async function reload() {
  loading.value = true
  try {
    const res = await promptApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { id: undefined, projectId: 1, name: '', code: '', description: '', status: 1 })
  dialogVisible.value = true
}

async function onEdit(row: PromptVO) {
  const r = await promptApi.get(row.id)
  Object.assign(form, r)
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) await promptApi.update(form)
  else await promptApi.create(form)
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: PromptVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]?`, '确认', { type: 'warning' })
  await promptApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

async function onVersions(row: PromptVO) {
  currentPrompt.value = row
  versions.value = await promptApi.versions(row.id)
  versionsDrawer.value = true
}

function onNewVersion() {
  if (!currentPrompt.value) return
  Object.assign(versionForm, { promptId: currentPrompt.value.id, content: '', changelog: '', activate: false })
  versionDialog.value = true
}

async function onSaveVersion() {
  if (!versionForm.content) {
    ElMessage.warning('请输入内容')
    return
  }
  await promptApi.createVersion(versionForm)
  ElMessage.success('已创建')
  versionDialog.value = false
  if (currentPrompt.value) {
    versions.value = await promptApi.versions(currentPrompt.value.id)
  }
}

async function onActivate(row: PromptVersionVO) {
  if (!currentPrompt.value) return
  await promptApi.activateVersion(currentPrompt.value.id, row.id)
  ElMessage.success('已激活 v' + row.version)
  if (currentPrompt.value) {
    versions.value = await promptApi.versions(currentPrompt.value.id)
  }
}

onMounted(reload)
</script>

<style scoped>
.versions-content { padding: 0 16px; }
.versions-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
