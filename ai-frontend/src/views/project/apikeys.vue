<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <el-button text @click="$router.push(`/project/${id}`)">
        <el-icon><ArrowLeft /></el-icon> 返回项目
      </el-button>
      <span class="page-title">API Key 管理</span>
      <div class="flex-spacer" />
      <el-button type="primary" :icon="Plus" @click="onCreate">创建 API Key</el-button>
    </div>

    <el-alert
      type="warning"
      :closable="false"
      title="API Key 用于外部系统调用本项目内的流程/助手,带限流保护。请妥善保管 secret,创建后只显示一次。"
      show-icon
      class="mb"
    />

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="apiKey" label="API Key" min-width="280">
        <template #default="{ row }">
          <code class="mono">{{ row.apiKey }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="maskedSecret" label="Secret" min-width="200">
        <template #default="{ row }">
          <code class="mono">{{ row.maskedSecret || '********' }}</code>
        </template>
      </el-table-column>
      <el-table-column label="Scope" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="s in row.scopes" :key="s" size="small" type="info" class="mr">
            {{ s }}
          </el-tag>
          <span v-if="!row.scopes || row.scopes.length === 0" class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="rateLimit" label="限流/分钟" width="110" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastUsedTime" label="最后使用" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="onReset(row)">重置</el-button>
          <el-button size="small" type="danger" @click="onRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit dialog -->
    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? '编辑 API Key' : '创建 API Key'"
      width="560px"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="editForm.name" placeholder="例如:CRM 集成" />
        </el-form-item>
        <el-form-item label="Scope">
          <el-input
            v-model="scopesText"
            placeholder="逗号分隔,例如: flow:run, assistant:chat(留空=不限制)"
          />
        </el-form-item>
        <el-form-item label="限流(次/分)">
          <el-input-number v-model="editForm.rateLimit" :min="0" :max="100000" />
        </el-form-item>
        <el-form-item v-if="editForm.id" label="状态">
          <el-switch
            v-model="editForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Created/Reset result dialog - shows plaintext secret once -->
    <el-dialog v-model="revealVisible" title="请保存凭据(只显示一次)" width="640px" :close-on-click-modal="false">
      <el-alert type="success" :closable="false" show-icon>
        创建/重置成功,请立即复制以下凭据,关闭后无法再次查看 Secret。
      </el-alert>
      <el-form label-width="100px" class="mt">
        <el-form-item label="API Key">
          <el-input v-model="reveal.apiKey" readonly>
            <template #append>
              <el-button @click="copyText(reveal.apiKey)">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="API Secret">
          <el-input v-model="reveal.apiSecret" readonly type="password" show-password>
            <template #append>
              <el-button @click="copyText(reveal.apiSecret)">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="调用示例">
          <pre class="example">{{ usageExample }}</pre>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="revealVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { apiKeyApi, type ApiKeyVO, type ApiKeySave } from '@/api/apikey'

const route = useRoute()
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
    ElMessage.warning('请输入名称')
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
    ElMessage.success('已更新')
    editVisible.value = false
    if (r.apiKey && r.apiSecret) {
      Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
      revealVisible.value = true
    }
  } else {
    const r = await apiKeyApi.create(id, payload)
    ElMessage.success('已创建')
    editVisible.value = false
    if (r.apiKey && r.apiSecret) {
      Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
      revealVisible.value = true
    }
  }
  load()
}

async function onReset(row: ApiKeyVO) {
  await ElMessageBox.confirm(`确定重置 [${row.name}] 的 Secret? 旧 Secret 立即失效。`, '确认', {
    type: 'warning'
  })
  const r = await apiKeyApi.reset(id, row.id)
  ElMessage.success('Secret 已重置')
  if (r.apiKey && r.apiSecret) {
    Object.assign(reveal, { apiKey: r.apiKey, apiSecret: r.apiSecret })
    revealVisible.value = true
  }
  load()
}

async function onRemove(row: ApiKeyVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]? 删除后无法恢复。`, '确认', { type: 'warning' })
  await apiKeyApi.remove(id, row.id)
  ElMessage.success('已删除')
  load()
}

function copyText(text: string) {
  if (!text) return
  navigator.clipboard?.writeText(text).then(
    () => ElMessage.success('已复制'),
    () => ElMessage.warning('复制失败,请手动选择')
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
