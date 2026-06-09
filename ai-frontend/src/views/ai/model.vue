<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">模型管理</span>
      <el-button v-if="can('ai:model:add')" type="primary" :icon="Plus" @click="onAdd">新增模型</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="名称 / 模型" clearable @keyup.enter="reload" />
      <el-select v-model="query.provider" placeholder="厂商" clearable>
        <el-option value="openai" label="OpenAI" />
        <el-option value="deepseek" label="DeepSeek" />
        <el-option value="claude" label="Claude" />
        <el-option value="qwen" label="通义千问" />
        <el-option value="glm" label="智谱 GLM" />
        <el-option value="ollama" label="Ollama" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="provider" label="厂商" width="120" />
      <el-table-column prop="modelName" label="模型" min-width="180" />
      <el-table-column prop="maxTokens" label="Max Tokens" width="120" />
      <el-table-column label="默认" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:model:view')" size="small" @click="onTest(row)">测试</el-button>
          <el-button v-if="can('ai:model:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('ai:model:delete')" size="small" type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑模型' : '新增模型'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="厂商" prop="provider">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option value="openai" label="OpenAI" />
            <el-option value="deepseek" label="DeepSeek" />
            <el-option value="claude" label="Claude" />
            <el-option value="qwen" label="通义千问" />
            <el-option value="glm" label="智谱 GLM" />
            <el-option value="ollama" label="Ollama" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型标识" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 gpt-4 / deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Base"><el-input v-model="form.apiBase" placeholder="留空用默认" /></el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="1" :max="32000" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-input v-model="form.embeddingModel" placeholder="如 text-embedding-3-small" />
        </el-form-item>
        <el-form-item label="向量维度">
          <el-input-number v-model="form.dimension" :min="0" :max="4096" />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { modelApi, type ModelVO, type ModelSave } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'

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
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择厂商', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}

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
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: ModelVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]?`, '确认', { type: 'warning' })
  await modelApi.remove(row.id)
  ElMessage.success('已删除')
  reload()
}

async function onTest(row: ModelVO) {
  ElMessage.info('正在测试...')
  const r = await modelApi.test(row.id)
  if (r.success) ElMessage.success('连接成功')
  else ElMessage.error('连接失败')
}

onMounted(reload)
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
