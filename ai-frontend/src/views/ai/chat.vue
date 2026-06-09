<template>
  <div class="chat-page">
    <el-container class="chat-container">
      <el-aside :width="showSessions ? '240px' : '0px'" class="chat-aside">
        <div class="session-list">
          <el-button type="primary" :icon="Plus" class="new-session" @click="onNewSession">新会话</el-button>
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: currentSessionId === s.id }"
            @click="selectSession(s)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="title">{{ s.title || '新会话' }}</span>
            <el-icon class="del" @click.stop="onDeleteSession(s)"><Delete /></el-icon>
          </div>
        </div>
      </el-aside>
      <el-container>
        <el-header class="chat-header">
          <el-button text @click="showSessions = !showSessions">
            <el-icon><Expand v-if="!showSessions" /><Fold v-else /></el-icon>
          </el-button>
          <span class="header-title">AI 对话</span>
          <el-select v-model="currentModelId" placeholder="选择模型" style="width: 240px" @change="onModelChange">
            <el-option v-for="m in models" :key="m.id" :value="m.id" :label="`${m.name} (${m.provider})`" />
          </el-select>
        </el-header>
        <el-main class="chat-main">
          <div v-if="messages.length === 0" class="empty">
            <el-icon size="64" color="#dcdfe6"><ChatDotRound /></el-icon>
            <p>开始与 AI 对话</p>
            <p class="hint">支持 OpenAI / DeepSeek / 通义 / 智谱 / Ollama 等</p>
          </div>
          <div v-else class="messages">
            <div
              v-for="(m, idx) in messages"
              :key="idx"
              class="message"
              :class="m.role"
            >
              <div class="avatar">
                <el-avatar v-if="m.role === 'user'" :size="32">U</el-avatar>
                <el-avatar v-else :size="32" type="primary">AI</el-avatar>
              </div>
              <div class="content">
                <pre>{{ m.content }}</pre>
                <div v-if="m.inputTokens" class="meta">
                  tokens: {{ m.inputTokens + m.outputTokens }} | {{ m.costMs }}ms
                </div>
              </div>
            </div>
            <div v-if="sending" class="message assistant streaming">
              <div class="avatar"><el-avatar :size="32" type="primary">AI</el-avatar></div>
              <div class="content">
                <pre>{{ streamingContent || '正在思考...' }}</pre>
              </div>
            </div>
          </div>
        </el-main>
        <el-footer class="chat-footer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="输入消息,Enter 发送,Shift+Enter 换行"
            @keydown.enter.exact.prevent="onSend"
            :disabled="!currentModelId"
          />
          <el-button type="primary" :loading="sending" :disabled="!currentModelId || !input.trim()" @click="onSend">
            发送
          </el-button>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatDotRound, Delete, Expand, Fold } from '@element-plus/icons-vue'
import { chatApi, type ChatSessionVO, type ChatMessageVO, type ChatResponse } from '@/api/ai/chat'
import { modelApi, type ModelVO } from '@/api/ai/model'

const sessions = ref<ChatSessionVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const currentSessionId = ref<number | null>(null)
const showSessions = ref(true)
const models = ref<ModelVO[]>([])
const currentModelId = ref<number | null>(null)
const input = ref('')
const sending = ref(false)
const streamingContent = ref('')

async function loadSessions() {
  sessions.value = await chatApi.sessions()
  if (sessions.value.length > 0 && !currentSessionId.value) {
    await selectSession(sessions.value[0])
  }
}

async function loadModels() {
  models.value = await modelApi.list()
  if (models.value.length > 0 && !currentModelId.value) {
    const def = models.value.find(m => m.isDefault === 1) || models.value[0]
    currentModelId.value = def.id
  }
}

async function selectSession(s: ChatSessionVO) {
  currentSessionId.value = s.id
  messages.value = await chatApi.messages(s.id)
  if (s.modelId) currentModelId.value = s.modelId
  await nextTick()
  scrollToBottom()
}

function onNewSession() {
  currentSessionId.value = null
  messages.value = []
}

async function onDeleteSession(s: ChatSessionVO) {
  await ElMessageBox.confirm(`删除会话 [${s.title || s.id}]?`, '确认', { type: 'warning' })
  await chatApi.removeSession(s.id)
  ElMessage.success('已删除')
  if (currentSessionId.value === s.id) {
    currentSessionId.value = null
    messages.value = []
  }
  await loadSessions()
}

function onModelChange() {
  // could persist to session
}

function scrollToBottom() {
  const el = document.querySelector('.chat-main')
  if (el) el.scrollTop = el.scrollHeight
}

async function onSend() {
  if (!input.value.trim() || !currentModelId.value) return
  const text = input.value
  input.value = ''
  sending.value = true
  streamingContent.value = ''

  // Add user message locally
  const userMsg: ChatMessageVO = {
    id: 0, sessionId: currentSessionId.value || 0, role: 'user', content: text, status: 1
  }
  messages.value.push(userMsg)
  await nextTick()
  scrollToBottom()

  try {
    const res: ChatResponse = await chatApi.send({
      modelId: currentModelId.value,
      message: text,
      sessionId: currentSessionId.value || undefined
    })
    if (!currentSessionId.value) currentSessionId.value = res.sessionId
    messages.value.push({
      id: res.messageId, sessionId: res.sessionId, role: 'assistant',
      content: res.content, inputTokens: res.inputTokens, outputTokens: res.outputTokens, costMs: res.costMs, status: 1
    })
    await loadSessions()
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
    streamingContent.value = ''
    await nextTick()
    scrollToBottom()
  }
}

onMounted(async () => {
  await loadModels()
  await loadSessions()
})
</script>

<style scoped>
.chat-page { padding: 0; }
.chat-container { height: calc(100vh - 56px - 32px); }
.chat-aside { background: #f5f7fa; border-right: 1px solid var(--ai-border); transition: width 0.2s; overflow: hidden; }
.session-list { padding: 12px; }
.new-session { width: 100%; margin-bottom: 12px; }
.session-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 4px;
  cursor: pointer; margin-bottom: 4px;
  font-size: 14px;
}
.session-item:hover { background: #e6e8eb; }
.session-item.active { background: var(--ai-primary); color: #fff; }
.session-item .title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-item .del { opacity: 0.5; }
.session-item .del:hover { opacity: 1; }
.chat-header {
  display: flex; align-items: center; gap: 12px;
  background: #fff; border-bottom: 1px solid var(--ai-border);
  padding: 0 16px; height: 56px;
}
.header-title { font-weight: 600; flex: 1; }
.chat-main {
  background: #fafbfc; padding: 16px 24px;
  overflow-y: auto;
}
.empty { text-align: center; padding-top: 80px; color: var(--ai-text-secondary); }
.empty p { margin: 8px 0; }
.empty .hint { font-size: 12px; color: #909399; }
.messages { display: flex; flex-direction: column; gap: 16px; max-width: 900px; margin: 0 auto; }
.message { display: flex; gap: 12px; }
.message.user { flex-direction: row-reverse; }
.message .avatar { flex-shrink: 0; }
.message .content {
  background: #fff; padding: 12px 16px; border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  max-width: 75%;
}
.message.user .content { background: var(--ai-primary); color: #fff; }
.message .content pre {
  margin: 0; white-space: pre-wrap; word-wrap: break-word;
  font-family: inherit;
}
.message .meta { font-size: 11px; opacity: 0.6; margin-top: 6px; }
.message.streaming .content { border: 1px solid var(--ai-primary); }

.chat-footer {
  display: flex; gap: 8px; align-items: flex-end;
  background: #fff; border-top: 1px solid var(--ai-border);
  padding: 12px 16px;
}
.chat-footer .el-textarea { flex: 1; }
</style>
