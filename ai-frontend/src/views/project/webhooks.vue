<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> {{ t('project.webhooks.back') }}
      </el-button>
      <span class="page-title">{{ t('project.webhooks.title') }}</span>
      <div class="flex-spacer" />
      <el-button type="primary" :icon="Plus" @click="onCreate">{{ t('project.webhooks.create') }}</el-button>
    </div>

    <el-alert type="info" :closable="false" class="mb">
      <template #title>{{ t('project.webhooks.alertTitle') }}</template>
      {{ t('project.webhooks.alertBody', { url: 'POST /api/v1/webhook/receive/{id}', sig: 'X-Webhook-Signature: sha256=<HMAC-SHA256(secret, body)>', ts: 'X-Webhook-Timestamp' }) }}
    </el-alert>

    <el-table :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="70" />
      <el-table-column :label="t('project.webhooks.colName')" prop="name" min-width="140" />
      <el-table-column :label="t('project.webhooks.colUrl')" prop="url" min-width="240" show-overflow-tooltip />
      <el-table-column :label="t('project.webhooks.colEvents')" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="e in row.events || []" :key="e" size="small" type="info" class="mr-4">{{ t('webhookEvent.' + e, e) }}</el-tag>
          <span v-if="!row.events || row.events.length === 0" class="muted">{{ t('project.webhooks.noEvents') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.updateTime')" prop="updateTime" width="180" />
      <el-table-column :label="t('common.action')" width="320" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button size="small" @click="onTest(row)">{{ t('project.webhooks.actionTest') }}</el-button>
          <el-button size="small" @click="onLogs(row)">{{ t('project.webhooks.actionLogs') }}</el-button>
          <el-button size="small" type="warning" @click="onResetSecret(row)">{{ t('project.webhooks.actionResetSecret') }}</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? t('project.webhooks.editDialogTitleEdit') : t('project.webhooks.editDialogTitleCreate')" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="t('project.webhooks.formName')" required>
          <el-input v-model="form.name" :placeholder="t('project.webhooks.formNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('project.webhooks.formUrl')" required>
          <el-input v-model="form.url" :placeholder="t('project.webhooks.formUrlPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('project.webhooks.formEvents')">
          <el-checkbox-group v-model="form.events">
            <el-checkbox v-for="o in WEBHOOK_EVENT_OPTIONS" :key="o.value" :value="o.value">{{ t('webhookEvent.' + o.value, o.label) }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="t('project.webhooks.formStatus')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('project.webhooks.formDesc')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-alert v-if="createdSecret" type="success" :closable="false" class="mb">
          <template #title>{{ t('project.webhooks.secretAlertTitle') }}</template>
          <code style="word-break: break-all">{{ createdSecret }}</code>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('project.webhooks.saveClose') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" :title="t('project.webhooks.testDialogTitle')" width="500px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item :label="t('project.webhooks.formEventType')">
          <el-select v-model="testForm.event" style="width: 100%">
            <el-option v-for="o in WEBHOOK_EVENT_OPTIONS" :key="o.value" :value="o.value" :label="t('webhookEvent.' + o.value, o.label)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('project.webhooks.formPayload')">
          <el-input v-model="testPayloadText" type="textarea" :rows="4" :placeholder="t('project.webhooks.formPayloadPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">{{ t('project.webhooks.saveClose') }}</el-button>
        <el-button type="primary" :loading="testSending" @click="onSendTest">{{ t('project.webhooks.testSend') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logsVisible" :title="t('project.webhooks.logsDialogTitle')" width="900px">
      <el-table :data="logRows" border stripe size="small" max-height="500">
        <el-table-column :label="t('common.id')" prop="id" width="60" />
        <el-table-column :label="t('project.webhooks.colEvent')" prop="event" width="200" />
        <el-table-column :label="t('project.webhooks.colStatusCode')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.responseStatus >= 200 && row.responseStatus < 300 ? 'success' : 'danger'">
              {{ row.responseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('project.webhooks.colRetry')" prop="retryCount" width="60" />
        <el-table-column :label="t('project.webhooks.colCost')" prop="costMs" width="90" />
        <el-table-column :label="t('project.webhooks.colTime')" prop="createTime" width="180" />
        <el-table-column :label="t('project.webhooks.colUrl')" prop="requestUrl" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="logPage.current"
        v-model:page-size="logPage.size"
        :total="logTotal"
        layout="total, prev, pager, next, jumper"
        class="mt"
        @current-change="loadLogs"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { webhookApi, WEBHOOK_EVENT_OPTIONS, type WebhookVO, type WebhookLog } from '@/api/webhook'

const route = useRoute()
const { t } = useI18n()
const id = Number(route.params.id)

const loading = ref(false)
const rows = ref<WebhookVO[]>([])

const editVisible = ref(false)
const form = reactive<WebhookVO>({
  id: undefined,
  name: '',
  url: '',
  events: [],
  status: 1,
  description: ''
})
const createdSecret = ref<string>('')

const testVisible = ref(false)
const testSending = ref(false)
const testForm = reactive({ event: 'webhook.test' })
const testPayloadText = ref('')
const testTargetId = ref<number | null>(null)

const logsVisible = ref(false)
const logRows = ref<WebhookLog[]>([])
const logTotal = ref(0)
const logPage = reactive({ current: 1, size: 20 })
const logTargetId = ref<number | null>(null)

const testPayload = computed(() => {
  if (!testPayloadText.value) return undefined
  try {
    return JSON.parse(testPayloadText.value)
  } catch {
    return undefined
  }
})

async function load() {
  loading.value = true
  try {
    rows.value = await webhookApi.list(id)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.url = ''
  form.events = []
  form.status = 1
  form.description = ''
  createdSecret.value = ''
}

function onCreate() {
  resetForm()
  editVisible.value = true
}

function onEdit(row: WebhookVO) {
  resetForm()
  form.id = row.id
  form.name = row.name
  form.url = row.url
  form.events = [...(row.events || [])]
  form.status = row.status ?? 1
  form.description = row.description || ''
  editVisible.value = true
}

async function onSave() {
  if (!form.name || !form.url) {
    ElMessage.warning(t('project.webhooks.required'))
    return
  }
  const payload: WebhookVO = {
    name: form.name,
    url: form.url,
    events: form.events || [],
    status: form.status,
    description: form.description
  }
  if (form.id) {
    await webhookApi.update(id, form.id, payload)
    ElMessage.success(t('project.webhooks.updated'))
  } else {
    const created = await webhookApi.create(id, payload)
    createdSecret.value = created.secret || ''
    ElMessage.success(createdSecret.value ? t('project.webhooks.createdWithSecret') : t('project.webhooks.created'))
  }
  load()
}

async function onRemove(row: WebhookVO) {
  await ElMessageBox.confirm(t('project.webhooks.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  await webhookApi.remove(id, row.id!)
  ElMessage.success(t('project.webhooks.removed'))
  load()
}

async function onResetSecret(row: WebhookVO) {
  await ElMessageBox.confirm(t('project.webhooks.resetConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  const updated = await webhookApi.resetSecret(id, row.id!)
  if (updated.secret) {
    resetForm()
    form.id = row.id
    form.name = row.name
    form.url = row.url
    form.events = [...(row.events || [])]
    form.status = row.status ?? 1
    form.description = row.description || ''
    createdSecret.value = updated.secret
    editVisible.value = true
  }
  ElMessage.success(t('project.webhooks.resetSuccess'))
  load()
}

function onTest(row: WebhookVO) {
  testTargetId.value = row.id!
  testForm.event = 'webhook.test'
  testPayloadText.value = ''
  testVisible.value = true
}

async function onSendTest() {
  if (!testTargetId.value) return
  testSending.value = true
  try {
    const res = await webhookApi.test(id, testTargetId.value, {
      event: testForm.event,
      payload: testPayload.value
    })
    ElMessage[res?.ok ? 'success' : 'warning'](res?.message || t('project.webhooks.created'))
  } finally {
    testSending.value = false
  }
}

async function onLogs(row: WebhookVO) {
  logTargetId.value = row.id!
  logPage.current = 1
  logsVisible.value = true
  await loadLogs()
}

async function loadLogs() {
  if (!logTargetId.value) return
  const res = await webhookApi.logs(id, logTargetId.value, logPage.current, logPage.size)
  logRows.value = res.records
  logTotal.value = res.total
}

watch(() => route.params.id, load, { immediate: true })
onMounted(load)
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.flex-spacer { flex: 1; }
.mb { margin-bottom: 12px; }
.mt { margin-top: 12px; }
.mr-4 { margin-right: 4px; }
.muted { color: #999; }
code { background: #f5f5f5; padding: 2px 4px; border-radius: 3px; font-size: 12px; }
</style>
