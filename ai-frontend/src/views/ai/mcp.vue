<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ t('ai.mcp.title') }}</span>
      <el-button v-if="can('ai:mcp:add')" type="primary" :icon="Plus" @click="onAdd">{{ t('ai.mcp.add') }}</el-button>
    </div>

    <el-alert :title="t('ai.mcp.backendPending')" type="warning" :closable="false" class="mb" />

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('ai.mcp.searchPlaceholder')" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">{{ t('common.search') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column :label="t('common.id')" prop="id" width="60" />
      <el-table-column :label="t('ai.mcp.colName')" prop="name" min-width="160" />
      <el-table-column :label="t('ai.mcp.colUrl')" prop="url" min-width="240" show-overflow-tooltip />
      <el-table-column :label="t('ai.mcp.colType')" prop="type" width="120" />
      <el-table-column :label="t('common.status')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ai.mcp.colCreateTime')" prop="createTime" width="180" />
      <el-table-column :label="t('common.action')" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:mcp:edit')" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="can('ai:mcp:delete')" size="small" type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? t('ai.mcp.editTitle') : t('ai.mcp.addTitle')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('ai.mcp.formName')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="t('ai.mcp.formUrl')" prop="url"><el-input v-model="form.url" placeholder="https://..." /></el-form-item>
        <el-form-item :label="t('ai.mcp.formType')">
          <el-select v-model="form.type" clearable style="width: 100%">
            <el-option value="stdio" label="stdio" />
            <el-option value="sse" label="sse" />
            <el-option value="http" label="http" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.mcp.formApiKey')"><el-input v-model="form.apiKey" type="password" show-password /></el-form-item>
        <el-form-item :label="t('ai.mcp.formDesc')"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item :label="t('ai.mcp.formStatus')">
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
import { mcpApi, type McpVO, type McpSave } from '@/api/ai/mcp'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()
const can = (p: string) => userStore.hasPermission(p)

const loading = ref(false)
const rows = ref<McpVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<McpSave>({
  name: '', url: '', type: 'http', apiKey: '', description: '', status: 1
})
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('ai.mcp.nameRequired'), trigger: 'blur' }],
  url: [{ required: true, message: t('ai.mcp.urlRequired'), trigger: 'blur' }]
}))

async function reload() {
  loading.value = true
  try {
    const res = await mcpApi.page(query)
    rows.value = res.records || []
    total.value = res.total || 0
  } catch {
    // TODO(backend): AiMcpController not registered yet — page will fall back to empty
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onAdd() {
  Object.assign(form, { id: undefined, name: '', url: '', type: 'http', apiKey: '', description: '', status: 1 })
  dialogVisible.value = true
}

async function onEdit(row: McpVO) {
  try {
    const r = await mcpApi.get(row.id)
    Object.assign(form, r)
  } catch {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (form.id) await mcpApi.update(form)
    else await mcpApi.create(form)
    ElMessage.success(t('ai.mcp.saved'))
    dialogVisible.value = false
    reload()
  } catch {
    // TODO(backend): swallow error until controller is wired
  }
}

async function onDelete(row: McpVO) {
  try {
    await ElMessageBox.confirm(t('ai.mcp.removeConfirm', { name: row.name }), t('common.confirm'), { type: 'warning' })
  } catch {
    return
  }
  try {
    await mcpApi.remove(row.id)
    ElMessage.success(t('ai.mcp.removed'))
    reload()
  } catch {
    // TODO(backend)
  }
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.mb { margin-bottom: 12px; }
</style>
