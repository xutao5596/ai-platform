<template>
  <el-config-provider :locale="elLocale">
    <div class="app-shell">
      <div v-if="showToggle" class="theme-toggle-fab">
        <el-tooltip :content="isDark ? 'Light mode' : 'Dark mode'" placement="left">
          <el-button circle @click="toggleTheme">
            <el-icon><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useLocale } from '@/composables/useLocale'
import { useTheme } from '@/composables/useTheme'

const { locale, setLocale } = useLocale()
const { isDark, toggleTheme } = useTheme()

const elLocale = computed(() => (locale.value === 'en-US' ? en : zhCn))
const showToggle = computed(() => !window.location.pathname.startsWith('/login'))

onMounted(() => {
  document.documentElement.setAttribute('lang', locale.value)
})

watch(locale, (val) => {
  document.documentElement.setAttribute('lang', val)
  setLocale(val)
})
</script>

<style scoped>
.app-shell { position: relative; min-height: 100%; }
.theme-toggle-fab {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 2000;
}
</style>
