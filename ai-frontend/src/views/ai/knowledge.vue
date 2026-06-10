<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('ai.knowledge.title') }}</span>
      <el-button v-if="can('ai:knowledge:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('ai.knowledge.add') }}</el-button>
    </div>

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
      <el-table-column :label="t('common.action')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:knowledge:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { knowledgeApi, type KnowledgeVO, type KnowledgeSave } from '@/api/ai/knowledge'
import { modelApi, type ModelVO } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'

const { t } = useI18n()
const userStore = useUserStore()
const appStore = useAppStore()
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

onMounted(async () => {
  embeddingModels.value = await modelApi.embedding()
  reload()
})
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
