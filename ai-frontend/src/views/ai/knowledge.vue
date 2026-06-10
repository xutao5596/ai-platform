<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('ai.knowledge.title') }}</span>
      <el-button v-if="can('ai:knowledge:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('ai.knowledge.add') }}</el-button>
    </div>

    <el-alert :title="t('ai.knowledge.backendPending')" type="warning" :closable="false" class="mb" />

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('ai.knowledge.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('ai.knowledge.colName')" prop="name" min-width="200" />
      <el-table-column :label="t('ai.knowledge.colDesc')" prop="description" min-width="240" />
      <el-table-column :label="t('ai.knowledge.colModel')" prop="modelId" width="120" />
      <el-table-column :label="t('ai.knowledge.colDocCount')" prop="docCount" width="100" />
      <el-table-column :label="t('ai.knowledge.colChunkCount')" prop="chunkCount" width="100" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:knowledge:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('ai:knowledge:edit')" size="small" type="primary" @click="onUpload(row)">{{ t('ai.knowledge.actionUpload') }}</el-button>
          <el-button v-if="can('ai:knowledge:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('ai.knowledge.editTitle') : t('ai.knowledge.addTitle')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('ai.knowledge.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('ai.knowledge.formProjectId')" prop="projectId"><el-input-number v-model="form.projectId" :min="1" /></el-form-item>
        <el-form-item :label="t('ai.knowledge.formDesc')"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item :label="t('ai.knowledge.formModel')">
          <el-select v-model="form.modelId" clearable style="width: 100%">
            <el-option v-for="m in embeddingModels" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.knowledge.formChunkSize')">
          <el-input-number v-model="form.chunkSize" :min="100" :max="5000" />
        </el-form-item>
        <el-form-item :label="t('ai.knowledge.formChunkOverlap')">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="500" />
        </el-form-item>
        <el-form-item :label="t('ai.knowledge.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" :title="t('ai.knowledge.uploadTitle', { name: uploadTarget?.name || '' })" width="520px">
      <p class="upload-hint">{{ t('ai.knowledge.uploadHint') }}</p>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-exceed="onExceed"
        accept=".pdf,.docx,.txt,.md"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">{{ t('ai.knowledge.uploadChoose') }}</div>
      </el-upload>
      <el-progress v-if="uploadProgress > 0 && uploadProgress < 100" :percentage="uploadProgress" class="mt" />
      <template #footer>
        <el-button @click="uploadDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">{{ t('ai.knowledge.uploadBtn') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="docDialogVisible" :title="t('ai.knowledge.uploadTitle', { name: uploadTarget?.name || '' })" width="640px">
      <el-table :data="docs" v-loading="docsLoading" border stripe>
        <el-table-column :label="t('ai.knowledge.colDocName')" prop="name" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('ai.knowledge.colDocSize')" prop="size" width="100">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column :label="t('ai.knowledge.colDocStatus')" prop="status" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'indexed' ? 'success' : 'info'">
              {{ row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('ai.knowledge.colDocTime')" prop="createTime" width="180" />
      </el-table>
      <el-empty v-if="!docsLoading && docs.length === 0" :description="t('ai.knowledge.noDoc')" />
      <template #footer>
        <el-button @click="docDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { knowledgeApi, type KnowledgeVO, type KnowledgeSave } from '@/api/ai/knowledge'
import { modelApi, type ModelVO } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<KnowledgeVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })
const embeddingModels = ref<ModelVO[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<KnowledgeSave>({
  projectId: 1, name: '', description: '', modelId: undefined, chunkSize: 500, chunkOverlap: 50, status: 1
})
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('ai.knowledge.nameRequired'), trigger: 'blur' }],
  projectId: [{ required: true, message: t('ai.knowledge.projectIdRequired'), trigger: 'blur' }]
}))

// Upload state
const uploadDialogVisible = ref(false)
const uploadTarget = ref<KnowledgeVO | null>(null)
const uploadFile = ref<File | null>(null)
const uploadProgress = ref(0)
const uploading = ref(false)
const uploadRef = ref<any>(null)

// Doc list state
const docDialogVisible = ref(false)
const docs = ref<Array<{ id?: number; name: string; size?: number; status?: string; createTime?: string }>>([])
const docsLoading = ref(false)

async function reload() {
  loading.value = true
  try {
    const list = await knowledgeApi.list()
    rows.value = list
    total.value = list.length
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { id: undefined, projectId: 1, name: '', description: '', modelId: undefined, chunkSize: 500, chunkOverlap: 50, status: 1 })
  dialogVisible.value = true
}

async function onEdit(row: KnowledgeVO) {
  const k = await knowledgeApi.get(row.id)
  Object.assign(form, k)
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) await knowledgeApi.update(form)
  else await knowledgeApi.create(form)
  ElMessage.success(t('ai.knowledge.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: KnowledgeVO) {
  await ElMessageBox.confirm(t('ai.knowledge.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await knowledgeApi.remove(row.id)
  ElMessage.success(t('ai.knowledge.removed'))
  reload()
}

function onUpload(row: KnowledgeVO) {
  uploadTarget.value = row
  uploadFile.value = null
  uploadProgress.value = 0
  uploading.value = false
  uploadDialogVisible.value = true
}

function onFileChange(file: { raw?: File }) {
  uploadFile.value = file.raw || null
}

function onExceed() {
  ElMessage.warning(t('ai.knowledge.uploadHint'))
}

async function submitUpload() {
  if (!uploadTarget.value) return
  if (!uploadFile.value) {
    ElMessage.warning(t('ai.knowledge.uploadChoose'))
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    await axios.post(`/api/v1/ai/knowledge/${uploadTarget.value.id}/doc/upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total) uploadProgress.value = Math.round((e.loaded * 100) / e.total)
      }
    })
    ElMessage.success(t('ai.knowledge.uploadSuccess'))
    uploadDialogVisible.value = false
    // open doc list dialog
    openDocList(uploadTarget.value)
    reload()
  } catch (e: any) {
    ElMessage.error(t('ai.knowledge.uploadFailed', { msg: e?.message || 'error' }))
  } finally {
    uploading.value = false
  }
}

async function openDocList(row: KnowledgeVO) {
  uploadTarget.value = row
  docDialogVisible.value = true
  docsLoading.value = true
  try {
    const res = await axios.get(`/api/v1/ai/knowledge/${row.id}/doc/list`)
    const data = res.data?.data ?? res.data
    docs.value = Array.isArray(data) ? data : []
  } catch {
    // TODO(backend): doc/list endpoint pending — show empty list
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

function formatSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(2) + 'MB'
}

onMounted(async () => {
  embeddingModels.value = await modelApi.embedding().catch(() => [])
  reload()
})
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.mb { margin-bottom: 12px; }
.mt { margin-top: 12px; }
.upload-hint { color: var(--ai-text-secondary); font-size: 12px; margin-bottom: 8px; }
</style>
