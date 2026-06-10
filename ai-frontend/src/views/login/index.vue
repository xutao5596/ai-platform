<template>
  <div class="login-container">
    <div class="login-bg" />
    <div class="login-box">
      <div class="login-header">
        <el-icon class="brand"><Cpu /></el-icon>
        <h1 class="title">{{ t('login.title') }}</h1>
        <p class="subtitle">{{ t('login.subtitle') }}</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('login.username')" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.password')"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="onSubmit">
            {{ t('login.submit') }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">
        <span>{{ t('login.defaultTip') }}</span>
        <code>{{ t('login.defaultAccount') }}</code>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { User, Lock, Cpu } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }]
}))

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success(t('login.success'))
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: any) {
    ElMessage.error(e?.message || t('login.failed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 50%, #06b6d4 100%);
  overflow: hidden;
}
.login-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 30%, rgba(99, 102, 241, 0.4), transparent 40%),
              radial-gradient(circle at 80% 70%, rgba(16, 185, 129, 0.3), transparent 40%);
}
.login-box {
  position: relative;
  width: 400px;
  padding: 40px;
  background: var(--ai-bg-elevated);
  color: var(--ai-text);
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}
.login-header { text-align: center; margin-bottom: 32px; }
.brand { font-size: 48px; color: var(--ai-primary); }
.title { font-size: 24px; font-weight: 600; margin: 12px 0 4px; color: var(--ai-text); }
.subtitle { font-size: 13px; color: var(--ai-text-secondary); }
.login-btn { width: 100%; height: 44px; font-size: 16px; letter-spacing: 4px; }
.login-tip {
  margin-top: 16px;
  font-size: 12px;
  color: var(--ai-text-secondary);
  text-align: center;
}
.login-tip code {
  background: var(--ai-bg-hover);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  color: var(--ai-primary);
}
</style>
