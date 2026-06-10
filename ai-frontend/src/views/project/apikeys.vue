<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> {{ t('project.apikeys.back') }}
      </el-button>
      <span class="page-title">{{ t('project.apikeys.title') }}</span>
      <div class="flex-spacer" />
      <el-button type="primary" :icon="Plus" @click="onCreate">{{ t('project.apikeys.create') }}</el-button>
    </div>

    <el-alert
      type="warning"
      :closable="false"
      :title="t('project.apikeys.alertTitle')"
      show-icon
      class="mb"
    />

    <el-table :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="80" />
      <el-table-column :label="t('project.apikeys.colName')" prop="name" min-width="160" />
      <el-table-column :label="t('project.apikeys.colApiKey')" prop="apiKey" min-width="280">
        <template #default="{ row }">
          <code class="mono">{{ row.apiKey }}</code>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.apikeys.colSecret')" prop="maskedSecret" min-width="200">
        <template #default="{ row }">
          <code class="mono">{{ row.maskedSecret || t('project.apikeys.noSecret') }}</code>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.apikeys.colScopes')" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="s in row.scopes" :key="s" size="small" type="info" class="mr">
            {{ s }}
          </el-tag>
          <span v-if="!row.scopes || row.scopes.length === 0" class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.apikeys.colRateLimit')" prop="rateLimit" width="110" align="center" />
      <el-table-column :label="t('common.status')" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('project.apikeys.colLastUsed')" prop="lastUsedTime" width="170" />
      <el-table-column :label="t('common.action')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button size="small" type="warning" @click="onReset(row)">{{ t('project.apikeys.actionReset') }}</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? t('project.apikeys.editDialogTitleEdit') : t('project.apikeys.editDialogTitleCreate')"
      width="560px"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item :label="t('project.apikeys.formName')" required>
          <el-input v-model="editForm.name" :placeholder="t('project.apikeys.formNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('project.apikeys.formScopes')">
          <el-input
            v-model="scopesText"
            :placeholder="t('project.apikeys.formScopesPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('project.apikeys.formRateLimit')">
          <el-input-number v-model="editForm.rateLimit" :min="0" :max="100000" />
        </el-form-item>
        <el-form-item v-if="editForm.id" :label="t('project.apikeys.formStatus')">
          <el-switch
            v-model="editForm.status"
            :active-value="1"
            :inactive-value="0"
            :active-text="t('project.apikeys.formSwitchActive')"
            :inactive-text="t('project.apikeys.formSwitchInactive')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revealVisible" :title="t('project.apikeys.revealDialogTitle')" width="640px" :close-on-click-modal="false">
      <el-alert type="success" :closable="false" show-icon>
        {{ t('project.apikeys.revealAlert') }}
      </el-alert>
      <el-form label-width="100px" class="mt">
        <el-form-item :label="t('project.apikeys.revealApiKey')">
          <el-input v-model="reveal.apiKey" readonly>
            <template #append>
              <el-button @click="copyText(reveal.apiKey)">{{ t('common.copy') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('project.apikeys.revealApiSecret')">
          <el-input v-model="reveal.apiSecret" readonly type="password" show-password>
            <template #append>
              <el-button @click="copyText(reveal.apiSecret)">{{ t('common.copy') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('project.apikeys.revealExample')">
          <pre class="example">{{ usageExample }}</pre>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="revealVisible = false">{{ t('project.apikeys.revealSaved') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { apiKeyApi, type ApiKeyVO, type ApiKeySave } from '@/api/apikey'

const route = useRoute()
const { t } = useI18n()
const id = Number(route.params.id)

const loading = ref(false)
const rows = ref<ApiKeyVO[]>([])

const editVisible = ref(false)
const editForm = reactive<ApiKeySave>({ name: '', scopes: [], rateLimit: 60, status: 1 })
const scopesText = ref('')

const revealVisible = ref(false)
const reveal = reactive<{ apiKey: string; apiSecret: string }>({ apiKey: '', apiSecret: '' })

const usageExample = computed(() => {
  if (!reveal.apiKey || !reveal.apiSecret) return ''
  return [
    'curl -X GET "http://localhost:8080/api/v1/ext/flow/list?projectId=' + id + '" \\',
    '  -H "Authorization: ApiKey ' + reveal.apiKey + ':' + reveal.apiSecret + '"'
  ].join('\n')
})

async function load() {
  loading.value = true
  try {
    rows.value = await apiKeyApi.list(id)
  } finally {
    loading.value = false
  }
}

function onCreate() {
  Object.assign(editForm, { id: undefined, name: '', scopes: [], rateLimit: 60, status: 1 })
  scopesText.value = ''
  editVisible.value = true
}

async function onEdit(row: ApiKeyVO) {
  Object.assign(editForm, {
    id: row.id,
    name: row.name,
    scopes: row.scopes || [],
    rateLimit: row.rateLimit || 60,
    status: row.status ?? 1
  })
  scopesText.value = (row.scopes || []).join(', ')
  editVisible.value = true
}

async function onSave() {
  if (!editForm.name) {
    ElMessage.warning(t('project.apikeys.nameRequired'))
    return
  }
  const scopes = scopesText.value
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
  const payload: ApiKeySave = {
    name: editForm.name,
    scopes,
    rateLimit: editForm.rateLimit,
    status: editForm.status
  }
  if (editForm.id) {
    const r = await apiKeyApi.update(id, editForm.id, payload)
    ElMessage.success(t('project.apikeys.updated'))
    editVisible.value = false
    if (r.apiKey && r.apiSecret) {
      Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
      revealVisible.value = true
    }
  } else {
    const r = await apiKeyApi.create(id, payload)
    ElMessage.success(t('project.apikeys.created'))
    editVisible.value = false
    if (r.apiKey && r.apiSecret) {
      Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
      revealVisible.value = true
    }
  }
  load()
}

async function onReset(row: ApiKeyVO) {
  await ElMessageBox.confirm(t('project.apikeys.resetConfirm', { name: row.name }), t('common.confirm'), {
    type: 'warning'
  })
  const r = await apiKeyApi.reset(id, row.id)
  ElMessage.success(t('project.apikeys.resetSuccess'))
  if (r.apiKey && r.apiSecret) {
    Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
    revealVisible.value = true
  }
  load()
}

async function onRemove(row: ApiKeyVO) {
  await ElMessageBox.confirm(t('project.apikeys.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await apiKeyApi.remove(id, row.id)
  ElMessage.success(t('project.apikeys.removed'))
  load()
}

function copyText(text: string) {
  if (!text) return
  navigator.clipboard?.writeText(text).then(
    () => ElMessage.success(t('common.copySuccess')),
    () => ElMessage.warning(t('common.copyFailed'))
  )
}

watch(() => route.params.id, load, { immediate: true })
onMounted(load)
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.flex-spacer { flex: 1; }
.mb { margin-bottom: 16px; }
.mt { margin-top: 12px; }
.mr { margin-right: 4px; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; }
.muted { color: var(--el-text-color-placeholder); }
.example {
  background: var(--el-fill-color-light);
  padding: 10px 12px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
