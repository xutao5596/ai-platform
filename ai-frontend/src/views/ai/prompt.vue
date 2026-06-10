<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('ai.prompt.title') }}</span>
      <el-button v-if="can('ai:prompt:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('ai.prompt.add') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('ai.prompt.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('ai.prompt.colName')" prop="name" min-width="180" />
      <el-table-column :label="t('ai.prompt.colCode')" prop="code" width="160" />
      <el-table-column :label="t('ai.prompt.colDesc')" prop="description" min-width="200" />
      <el-table-column :label="t('ai.prompt.colVersionCount')" prop="versionCount" width="100" />
      <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:prompt:edit')" size="small" @click="onVersions(row)">{{ t('ai.prompt.actionVersions') }}</el-button>
          <el-button v-if="can('ai:prompt:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('ai:prompt:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('ai.prompt.editTitle') : t('ai.prompt.addTitle')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('ai.prompt.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('ai.prompt.formProjectId')" prop="projectId"><el-input-number v-model="form.projectId" :min="1" /></el-form-item>
        <el-form-item :label="t('ai.prompt.formCode')" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item :label="t('ai.prompt.formDesc')"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item :label="t('ai.prompt.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionsDrawer" :title="t('ai.prompt.versionsTitle')" direction="rtl" size="60%">
      <div v-if="currentPrompt" class="versions-content">
        <div class="versions-header">
          <h3>{{ currentPrompt.name }} ({{ currentPrompt.code }})</h3>
          <el-button v-if="can('ai:prompt:add')" type="primary" :icon="Plus" @click="onNewVersion">{{ t('ai.prompt.newVersion') }}</el-button>
        </div>
        <el-table :data="versions" border>
          <el-table-column :label="t('ai.prompt.colVersion')" prop="version" width="80" />
          <el-table-column :label="t('ai.prompt.colChange')" prop="changelog" />
          <el-table-column :label="t('ai.prompt.colActive')" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="success" size="small">{{ t('ai.prompt.activeTag') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="160">
            <template #default="{ row }">
              <el-button v-if="row.isActive !== 1" size="small" @click="onActivate(row)">{{ t('ai.prompt.activate') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="versionDialog" :title="versionForm.id ? t('ai.prompt.editVersionTitle') : t('ai.prompt.newVersionTitle')" width="700px">
      <el-form :model="versionForm" label-width="100px">
        <el-form-item :label="t('ai.prompt.formContent')">
          <el-input v-model="versionForm.content" type="textarea" :rows="10" :placeholder="t('ai.prompt.formContentPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.prompt.formChangelog')"><el-input v-model="versionForm.changelog" /></el-form-item>
        <el-form-item :label="t('ai.prompt.formActivate')"><el-switch v-model="versionForm.activate" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveVersion">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { promptApi, type PromptVO, type PromptSave, type PromptVersionVO, type PromptVersionSave } from '@/api/ai/prompt'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<PromptVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PromptSave>({ projectId: 1, name: '', code: '', description: '', status: 1 })
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('ai.prompt.nameRequired'), trigger: 'blur' }],
  projectId: [{ required: true, message: t('ai.prompt.projectIdRequired'), trigger: 'blur' }],
  code: [{ required: true, message: t('ai.prompt.codeRequired'), trigger: 'blur' }]
}))

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
  ElMessage.success(t('ai.prompt.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: PromptVO) {
  await ElMessageBox.confirm(t('ai.prompt.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await promptApi.remove(row.id)
  ElMessage.success(t('ai.prompt.removed'))
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
    ElMessage.warning(t('ai.prompt.contentRequired'))
    return
  }
  await promptApi.createVersion(versionForm)
  ElMessage.success(t('ai.prompt.versionCreated'))
  versionDialog.value = false
  if (currentPrompt.value) {
    versions.value = await promptApi.versions(currentPrompt.value.id)
  }
}

async function onActivate(row: PromptVersionVO) {
  if (!currentPrompt.value) return
  await promptApi.activateVersion(currentPrompt.value.id, row.id)
  ElMessage.success(t('ai.prompt.versionActivated', { version: row.version }))
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
