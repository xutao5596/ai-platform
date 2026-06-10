<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('ai.model.title') }}</span>
      <el-button v-if="can('ai:model:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('ai.model.add') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('ai.model.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-select v-model="query.provider" :placeholder="t('ai.model.filterProvider')" clearable>
        <el-option value="openai" :label="t('ai.model.providerOpenai')" />
        <el-option value="deepseek" :label="t('ai.model.providerDeepseek')" />
        <el-option value="claude" :label="t('ai.model.providerClaude')" />
        <el-option value="qwen" :label="t('ai.model.providerQwen')" />
        <el-option value="glm" :label="t('ai.model.providerGlm')" />
        <el-option value="ollama" :label="t('ai.model.providerOllama')" />
      </el-select>
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('ai.model.colName')" prop="name" width="160" />
      <el-table-column :label="t('ai.model.colProvider')" prop="provider" width="120" />
      <el-table-column :label="t('ai.model.colModel')" prop="modelName" min-width="180" />
      <el-table-column :label="t('ai.model.colMaxTokens')" prop="maxTokens" width="120" />
      <el-table-column :label="t('ai.model.colDefault')" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success" size="small">{{ t('ai.model.defaultTag') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:model:view')" size="small" @click="onTest(row)">{{ t('ai.model.actionTest') }}</el-button>
          <el-button v-if="can('ai:model:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('ai:model:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @current-change="reload"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? t('ai.model.editTitle') : t('ai.model.addTitle')" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('ai.model.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('ai.model.formProvider')" prop="provider">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option value="openai" :label="t('ai.model.providerOpenai')" />
            <el-option value="deepseek" :label="t('ai.model.providerDeepseek')" />
            <el-option value="claude" :label="t('ai.model.providerClaude')" />
            <el-option value="qwen" :label="t('ai.model.providerQwen')" />
            <el-option value="glm" :label="t('ai.model.providerGlm')" />
            <el-option value="ollama" :label="t('ai.model.providerOllama')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.model.formModelName')" prop="modelName">
          <el-input v-model="form.modelName" :placeholder="t('ai.model.formModelNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formApiBase')"><el-input v-model="form.apiBase" :placeholder="t('ai.model.formApiBasePlaceholder')" /></el-form-item>
        <el-form-item :label="t('ai.model.formApiKey')" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('ai.model.formMaxTokens')">
          <el-input-number v-model="form.maxTokens" :min="1" :max="32000" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formTemperature')">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formEmbeddingModel')">
          <el-input v-model="form.embeddingModel" :placeholder="t('ai.model.formEmbeddingModelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formDimension')">
          <el-input-number v-model="form.dimension" :min="0" :max="4096" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formIsDefault')">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('ai.model.formDesc')"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
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
import { modelApi, type ModelVO, type ModelSave } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<ModelVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', provider: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ModelSave>({
  name: '', provider: 'openai', modelName: '', apiKey: '',
  maxTokens: 4096, temperature: 0.7, isDefault: 0, status: 1
})
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('ai.model.nameRequired'), trigger: 'blur' }],
  provider: [{ required: true, message: t('ai.model.providerRequired'), trigger: 'change' }],
  modelName: [{ required: true, message: t('ai.model.modelNameRequired'), trigger: 'blur' }],
  apiKey: [{ required: true, message: t('ai.model.apiKeyRequired'), trigger: 'blur' }]
}))

async function reload() {
  loading.value = true
  try {
    const res = await modelApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { id: undefined, name: '', provider: 'openai', modelName: '', apiKey: '', apiBase: '', maxTokens: 4096, temperature: 0.7, embeddingModel: '', dimension: 0, isDefault: 0, status: 1, description: '' })
  dialogVisible.value = true
}

async function onEdit(row: ModelVO) {
  const r = await modelApi.get(row.id)
  Object.assign(form, r)
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) await modelApi.update(form)
  else await modelApi.create(form)
  ElMessage.success(t('ai.model.saved'))
  dialogVisible.value = false
  reload()
}

async function onDelete(row: ModelVO) {
  await ElMessageBox.confirm(t('ai.model.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await modelApi.remove(row.id)
  ElMessage.success(t('ai.model.removed'))
  reload()
}

async function onTest(row: ModelVO) {
  ElMessage.info(t('ai.model.testing'))
  const r = await modelApi.test(row.id)
  if (r.success) ElMessage.success(t('ai.model.testSuccess'))
  else ElMessage.error(t('ai.model.testFailed'))
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
