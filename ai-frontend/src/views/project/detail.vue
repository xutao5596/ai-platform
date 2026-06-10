<template>
  <div v-loading="loading" class="page-container">
    <div class="detail-header">
      <el-button text @click="$router.push('/project')">
        <el-icon><ArrowLeft /></el-icon> {{ t('project.detail.back') }}
      </el-button>
      <h2 class="title">{{ project?.name || t('project.detail.defaultTitle') }}</h2>
      <div class="flex-spacer" />
      <el-button @click="$router.push(`/project/${id}/members`)">{{ t('project.detail.membersBtn') }}</el-button>
      <el-button @click="$router.push(`/project/${id}/apikeys`)">{{ t('project.detail.apiKeysBtn') }}</el-button>
      <el-button @click="$router.push(`/project/${id}/webhooks`)">{{ t('project.detail.webhooksBtn') }}</el-button>

    </div>

    <el-tabs v-model="active" class="mt" @tab-change="onTabChange">
      <el-tab-pane :label="t('project.detail.tabOverview')" name="overview">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('project.detail.labelName')">{{ project?.name }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelCode')">{{ project?.code }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelDesc')" :span="2">{{ project?.description || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelRole')">
            <el-tag v-if="project?.roleCode" :type="roleTagType(project.roleCode) as any">{{ t('project.role.' + project.roleCode) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelMemberCount')">{{ project?.memberCount }}</el-descriptions-item>
          <el-descriptions-item :label="t('project.detail.labelCreateTime')">{{ project?.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabFlow')" name="flow">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('project.detail.flowListTitle') }}</span>
          <el-button type="primary" :icon="Plus" size="small" @click="onCreateFlow">
            {{ t('project.detail.createFlow') }}
          </el-button>
        </div>
        <el-table v-loading="flowLoading" :data="flows" border stripe>
          <el-table-column :label="t('project.detail.colFlowName')" min-width="200">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/flow/${row.id}`)">{{ row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colFlowType')" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.isAssistant === 1" type="warning" size="small">{{ t('flow.list.typeTagAssistant') }}</el-tag>
              <el-tag v-else type="info" size="small">{{ t('flow.list.typeTagNormal') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? t('flow.list.statusPublished') : t('flow.list.statusDraft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colCreateTime')" prop="createTime" width="180" />
          <el-table-column :label="t('common.action')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/flow/${row.id}/editor`)">{{ t('flow.list.actionEdit') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!flowLoading && flows.length === 0" :description="t('project.detail.flowEmpty')" />
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabKnowledge')" name="knowledge">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('project.detail.kbListTitle') }}</span>
          <el-button type="primary" :icon="Plus" size="small" @click="kbDialogVisible = true">
            {{ t('project.detail.createKb') }}
          </el-button>
        </div>
        <el-table v-loading="kbLoading" :data="knowledgeList" border stripe>
          <el-table-column :label="t('project.detail.colKbName')" prop="name" min-width="200" />
          <el-table-column :label="t('project.detail.colKbDesc')" prop="description" min-width="240" show-overflow-tooltip />
          <el-table-column :label="t('project.detail.colKbDocCount')" prop="docCount" width="100" />
          <el-table-column :label="t('project.detail.colKbStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colCreateTime')" prop="createTime" width="180" />
        </el-table>
        <el-empty v-if="!kbLoading && knowledgeList.length === 0" :description="t('project.detail.kbEmpty')" />

        <el-dialog v-model="kbDialogVisible" :title="t('project.detail.createKbTitle')" width="480px">
          <el-form ref="kbFormRef" :model="kbForm" :rules="kbRules" label-width="90px">
            <el-form-item :label="t('project.detail.colKbName')" prop="name">
              <el-input v-model="kbForm.name" />
            </el-form-item>
            <el-form-item :label="t('project.detail.colKbDesc')">
              <el-input v-model="kbForm.description" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="kbDialogVisible = false">{{ t('common.cancel') }}</el-button>
            <el-button type="primary" @click="onSaveKb">{{ t('common.create') }}</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabAssistant')" name="assistant">
        <div class="tab-toolbar">
          <span class="tab-title">{{ t('project.detail.assistantListTitle') }}</span>
          <el-button type="primary" :icon="Plus" size="small" @click="assistantDialogVisible = true">
            {{ t('project.detail.createAssistant') }}
          </el-button>
        </div>
        <el-table v-loading="assistantLoading" :data="assistants" border stripe>
          <el-table-column :label="t('project.detail.colAssistantName')" min-width="200">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/assistant`)">{{ row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colAssistantDesc')" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.description || (row.persona ? row.persona.slice(0, 60) : '-') }}
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colAssistantStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('project.detail.colCreateTime')" prop="createTime" width="180" />
        </el-table>
        <el-empty v-if="!assistantLoading && assistants.length === 0" :description="t('project.detail.assistantEmpty')" />

        <el-dialog v-model="assistantDialogVisible" :title="t('project.detail.createAssistantTitle')" width="520px">
          <el-form ref="assistantFormRef" :model="assistantForm" :rules="assistantRules" label-width="100px">
            <el-form-item :label="t('project.detail.colAssistantName')" prop="name">
              <el-input v-model="assistantForm.name" />
            </el-form-item>
            <el-form-item :label="t('project.detail.colAssistantDesc')">
              <el-input v-model="assistantForm.description" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item :label="t('project.detail.colAssistantPersona')">
              <el-input v-model="assistantForm.persona" type="textarea" :rows="3" :placeholder="t('project.detail.personaPlaceholder')" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="assistantDialogVisible = false">{{ t('common.cancel') }}</el-button>
            <el-button type="primary" @click="onSaveAssistant">{{ t('common.create') }}</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane :label="t('project.detail.tabSettings')" name="settings">
        <el-form
          v-if="settingsForm"
          ref="settingsFormRef"
          :model="settingsForm"
          :rules="settingsRules"
          label-width="100px"
          style="max-width: 640px"
        >
          <el-form-item :label="t('project.detail.colSettingsName')" prop="name">
            <el-input v-model="settingsForm.name" />
          </el-form-item>
          <el-form-item :label="t('project.detail.colSettingsCode')">
            <el-input v-model="settingsForm.code" :placeholder="t('project.detail.codeHint')" />
          </el-form-item>
          <el-form-item :label="t('project.detail.colSettingsDesc')">
            <el-input v-model="settingsForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="t('project.detail.colSettingsStatus')">
            <el-switch
              v-model="settingsForm.status"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('common.enabled')"
              :inactive-text="t('common.disabled')"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="settingsSaving" @click="onSaveSettings">
              {{ t('common.save') }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { projectApi, type ProjectVO, type ProjectSave } from '@/api/project'
import { flowApi } from '@/api/flow'
import { knowledgeApi, type KnowledgeVO, type KnowledgeSave } from '@/api/ai/knowledge'
import { assistantApi } from '@/api/assistant'
import type { FlowVO } from '@/types/flow'
import type { AssistantVO, AssistantSave } from '@/types/assistant'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const id = Number(route.params.id)
const project = ref<ProjectVO | null>(null)
const loading = ref(false)
const active = ref('overview')

const roleTagType = (c: string) => ({ owner: 'danger', admin: 'warning', developer: 'success', viewer: 'info' }[c] || '')

async function load() {
  loading.value = true
  try {
    project.value = await projectApi.get(id)
  } finally {
    loading.value = false
  }
}

const flows = ref<FlowVO[]>([])
const flowLoading = ref(false)
async function loadFlows() {
  flowLoading.value = true
  try {
    flows.value = await flowApi.list(id)
  } catch {
    flows.value = []
  } finally {
    flowLoading.value = false
  }
}
function onCreateFlow() {
  router.push({ path: '/flow', query: { projectId: String(id), action: 'new' } })
}

const knowledgeList = ref<KnowledgeVO[]>([])
const kbLoading = ref(false)
const kbDialogVisible = ref(false)
const kbFormRef = ref<FormInstance>()
const kbForm = reactive<KnowledgeSave>({ projectId: id, name: '', description: '' })
const kbRules = computed<FormRules>(() => ({ name: [{ required: true, message: t('project.detail.kbNameRequired'), trigger: 'blur' }] }))

async function loadKnowledge() {
  kbLoading.value = true
  try {
    knowledgeList.value = await knowledgeApi.list(id)
  } catch {
    knowledgeList.value = []
  } finally {
    kbLoading.value = false
  }
}
async function onSaveKb() {
  if (!kbFormRef.value) return
  const valid = await kbFormRef.value.validate().catch(() => false)
  if (!valid) return
  await knowledgeApi.create({ ...kbForm, projectId: id })
  ElMessage.success(t('project.detail.kbCreated'))
  kbDialogVisible.value = false
  kbForm.name = ''
  kbForm.description = ''
  loadKnowledge()
}

const assistants = ref<AssistantVO[]>([])
const assistantLoading = ref(false)
const assistantDialogVisible = ref(false)
const assistantFormRef = ref<FormInstance>()
const assistantForm = reactive<AssistantSave>({ projectId: id, name: '', description: '', persona: '' } as AssistantSave)
const assistantRules = computed<FormRules>(() => ({ name: [{ required: true, message: t('project.detail.assistantNameRequired'), trigger: 'blur' }] }))

async function loadAssistants() {
  assistantLoading.value = true
  try {
    assistants.value = await assistantApi.list(id)
  } catch {
    assistants.value = []
  } finally {
    assistantLoading.value = false
  }
}
async function onSaveAssistant() {
  if (!assistantFormRef.value) return
  const valid = await assistantFormRef.value.validate().catch(() => false)
  if (!valid) return
  await assistantApi.create({ ...assistantForm, projectId: id })
  ElMessage.success(t('project.detail.assistantCreated'))
  assistantDialogVisible.value = false
  assistantForm.name = ''
  assistantForm.description = ''
  assistantForm.persona = ''
  loadAssistants()
}

const settingsForm = ref<ProjectSave | null>(null)
const settingsFormRef = ref<FormInstance>()
const settingsSaving = ref(false)
const settingsRules = computed<FormRules>(() => ({ name: [{ required: true, message: t('project.detail.settingsNameRequired'), trigger: 'blur' }] }))

function loadSettings() {
  if (!project.value) return
  settingsForm.value = {
    id: project.value.id,
    name: project.value.name,
    code: project.value.code,
    description: project.value.description,
    status: project.value.status
  }
}
async function onSaveSettings() {
  if (!settingsFormRef.value || !settingsForm.value) return
  const valid = await settingsFormRef.value.validate().catch(() => false)
  if (!valid) return
  settingsSaving.value = true
  try {
    await projectApi.update(settingsForm.value)
    ElMessage.success(t('project.detail.settingsSaved'))
    await load()
    loadSettings()
  } finally {
    settingsSaving.value = false
  }
}

const loaded = { flow: false, knowledge: false, assistant: false, settings: false }
function onTabChange(name: string | number | undefined) {
  if (!name) return
  if (name === 'flow' && !loaded.flow) { loaded.flow = true; loadFlows() }
  else if (name === 'knowledge' && !loaded.knowledge) { loaded.knowledge = true; loadKnowledge() }
  else if (name === 'assistant' && !loaded.assistant) { loaded.assistant = true; loadAssistants() }
  else if (name === 'settings' && !loaded.settings) { loaded.settings = true; loadSettings() }
}

watch(() => route.params.id, () => {
  loaded.flow = false
  loaded.knowledge = false
  loaded.assistant = false
  loaded.settings = false
  load()
}, { immediate: true })
onMounted(load)
</script>

<style scoped>
.detail-header { display: flex; align-items: center; gap: 12px; }
.title { margin: 0; font-size: 20px; color: var(--ai-text); }
.mt { margin-top: 16px; }
.flex-spacer { flex: 1; }
.tab-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.tab-title { font-size: 15px; font-weight: 500; color: var(--ai-text); }
</style>
