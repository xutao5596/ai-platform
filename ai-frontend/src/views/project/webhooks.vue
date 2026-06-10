<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> 返回项目
      </el-button>
      <span class="page-title">Webhook 管理</span>
      <div class="flex-spacer" />
      <el-button type="primary" :icon="Plus" @click="onCreate">新建 Webhook</el-button>
    </div>

    <el-alert type="info" :closable="false" class="mb">
      <template #title>接收端点</template>
      外部系统向 <code>POST /api/v1/webhook/receive/&#123;id&#125;</code> 发送事件,需携带
      <code>X-Webhook-Signature: sha256=&lt;HMAC-SHA256(secret, body)&gt;</code> 与
      <code>X-Webhook-Timestamp</code>(5 分钟内有效)。
    </el-alert>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="url" label="URL" min-width="240" show-overflow-tooltip />
      <el-table-column label="订阅事件" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="e in row.events || []" :key="e" size="small" type="info" class="mr-4">{{ e }}</el-tag>
          <span v-if="!row.events || row.events.length === 0" class="muted">未订阅</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" @click="onTest(row)">测试</el-button>
          <el-button size="small" @click="onLogs(row)">日志</el-button>
          <el-button size="small" type="warning" @click="onResetSecret(row)">重置密钥</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 / 编辑 dialog -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑 Webhook' : '新建 Webhook'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如:订单通知" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="form.url" placeholder="https://example.com/hook" />
        </el-form-item>
        <el-form-item label="订阅事件">
          <el-checkbox-group v-model="form.events">
            <el-checkbox v-for="o in WEBHOOK_EVENT_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-alert v-if="createdSecret" type="success" :closable="false" class="mb">
          <template #title>secret 仅此一次显示,请妥善保存</template>
          <code style="word-break: break-all">{{ createdSecret }}</code>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">关闭</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试 dialog -->
    <el-dialog v-model="testVisible" title="发送测试事件" width="500px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="事件类型">
          <el-select v-model="testForm.event" style="width: 100%">
            <el-option v-for="o in WEBHOOK_EVENT_OPTIONS" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="附加 payload">
          <el-input v-model="testPayloadText" type="textarea" :rows="4" placeholder='{"key":"value"} 的 JSON 字符串' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testSending" @click="onSendTest">发送</el-button>
      </template>
    </el-dialog>

    <!-- 日志 dialog -->
    <el-dialog v-model="logsVisible" title="投递日志" width="900px">
      <el-table :data="logRows" border stripe size="small" max-height="500">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="event" label="事件" width="200" />
        <el-table-column label="状态码" width="90">
          <template #default="{ row }">
            <el-tag :type="row.responseStatus >= 200 && row.responseStatus < 300 ? 'success' : 'danger'">
              {{ row.responseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="60" />
        <el-table-column prop="costMs" label="耗时(ms)" width="90" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="requestUrl" label="URL" min-width="200" show-overflow-tooltip />
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
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { webhookApi, WEBHOOK_EVENT_OPTIONS, type WebhookVO, type WebhookLog } from '@/api/webhook'

const route = useRoute()
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
    ElMessage.warning('名称和 URL 不能为空')
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
    ElMessage.success('已更新')
  } else {
    const created = await webhookApi.create(id, payload)
    createdSecret.value = created.secret || ''
    ElMessage.success(createdSecret.value ? '已创建,secret 已显示' : '已创建')
  }
  load()
}

async function onRemove(row: WebhookVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]?`, '确认', { type: 'warning' })
  await webhookApi.remove(id, row.id!)
  ElMessage.success('已删除')
  load()
}

async function onResetSecret(row: WebhookVO) {
  await ElMessageBox.confirm(`确定重置 [${row.name}] 的 secret?旧值将立即失效。`, '确认', { type: 'warning' })
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
  ElMessage.success('secret 已重置')
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
    ElMessage[res?.ok ? 'success' : 'warning'](res?.message || '已发送')
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
