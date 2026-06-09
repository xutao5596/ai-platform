<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">知识库</span>
      <el-button v-if="can('ai:knowledge:add')" type="primary" :icon="Plus" @click="onAdd">新建知识库</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="名称" clearable @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="240" />
      <el-table-column prop="modelId" label="Embedding 模型" width="120" />
      <el-table-column prop="docCount" label="文档数" width="100" />
      <el-table-column prop="chunkCount" label="分段数" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('ai:knowledge:edit')" size="small" @click="onEdit(row)">编辑</el-button>
          <el-button v-if="can('ai:knowledge:delete')" size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识库' : '新建知识库'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="项目 ID" prop="projectId"><el-input-number v-model="form.projectId" :min="1" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="Embedding 模型">
          <el-select v-model="form.modelId" clearable style="width: 100%">
            <el-option v-for="m in embeddingModels" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分段大小">
          <el-input-number v-model="form.chunkSize" :min="100" :max="5000" />
        </el-form-item>
        <el-form-item label="分段重叠">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
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
import { knowledgeApi, type KnowledgeVO, type KnowledgeSave } from '@/api/ai/knowledge'
import { modelApi, type ModelVO } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'

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
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '项目 ID 必填', trigger: 'blur' }]
}

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
  ElMessage.success('已保存')
  dialogVisible.value = false
  reload()
}

async function onDelete(row: KnowledgeVO) {
  await ElMessageBox.confirm(`确定删除 [${row.name}]?`, '确认', { type: 'warning' })
  await knowledgeApi.remove(row.id)
  ElMessage.success('已删除')
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
