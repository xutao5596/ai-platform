<template>
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <el-icon class="logo-icon"><Cpu /></el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">AI Platform</span>
      </div>
      <el-menu
        :default-active="activePath"
        :collapse="appStore.sidebarCollapsed"
        router
        class="sidebar-menu"
        :style="{ '--el-menu-bg-color': 'var(--ai-sidebar-bg)', '--el-menu-text-color': 'var(--ai-sidebar-text)', '--el-menu-active-color': 'var(--ai-sidebar-active)', '--el-menu-hover-bg-color': 'var(--ai-sidebar-hover)' }"
      >
        <template v-for="m in menus" :key="m.id">
          <el-menu-item :index="resolvePath(m)">
            <el-icon><component :is="resolveIcon(m.icon)" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text @click="appStore.toggleSidebar">
            <el-icon><Expand v-if="appStore.sidebarCollapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">{{ t('nav.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.titleKey">{{ t(route.meta.titleKey as string) }}</el-breadcrumb-item>
            <el-breadcrumb-item v-else-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="onLangCommand">
            <span class="lang-trigger">
              <el-icon><Position /></el-icon>
              <span class="lang-label">{{ currentLangLabel }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">简体中文</el-dropdown-item>
                <el-dropdown-item command="en-US" :disabled="locale === 'en-US'">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown @command="onCommand">
            <span class="user-trigger">
              <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="user-name">{{ userStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ t('layout.profile') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ t('layout.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { useLocale } from '@/composables/useLocale'
import type { LocaleKey } from '@/locales'
import { Cpu, Expand, Fold, ArrowDown, Position } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const { t } = useI18n()
const { locale, setLocale } = useLocale()

const activePath = computed(() => route.path)

const currentLangLabel = computed(() => (locale.value === 'en-US' ? 'English' : '简体中文'))

function onLangCommand(cmd: string) {
  if (cmd === 'zh-CN' || cmd === 'en-US') {
    setLocale(cmd as LocaleKey)
  }
}

const avatarText = computed(() => {
  const n = userStore.userInfo?.realName || userStore.userInfo?.username || '?'
  return n.charAt(0).toUpperCase()
})

const menus = computed(() => {
  return (appStore.menus || []).filter((m: any) => !m.hidden && m.status === 1)
})

const iconMap: Record<string, string> = {
  HomeFilled: 'HomeFilled',
  Folder: 'Folder',
  MagicStick: 'MagicStick',
  Monitor: 'Monitor',
  Setting: 'Setting',
  ChatDotRound: 'ChatDotRound',
  User: 'User',
  UserFilled: 'UserFilled',
  Menu: 'Menu',
  OfficeBuilding: 'OfficeBuilding',
  Collection: 'Collection',
  Document: 'Document',
  Connection: 'Connection',
  Reading: 'Reading'
}
function resolveIcon(icon?: string) {
  if (!icon) return 'Menu'
  return iconMap[icon] || 'Menu'
}

function resolvePath(m: any) {
  const p = m.path || ''
  if (p.startsWith('/')) return p
  return '/system/' + p
}

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    ElMessage.info(t('layout.profileTip'))
  }
}

onMounted(async () => {
  if (appStore.menus.length === 0) {
    try {
      await appStore.fetchMenus()
    } catch (e) {
      // ignore
    }
  }
})
</script>

<style scoped>
.layout { height: 100vh; }
.sidebar {
  background: var(--ai-sidebar-bg);
  transition: width 0.2s;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: var(--ai-sidebar-text);
  border-bottom: 1px solid var(--ai-sidebar-hover);
}
.logo-icon { font-size: 22px; color: var(--ai-primary); }
.logo-text { font-size: 16px; font-weight: 600; white-space: nowrap; }
.sidebar-menu { border-right: none; }
.sidebar-menu:not(.el-menu--collapse) { width: 200px; }
.sidebar-menu :deep(.el-menu) {
  background-color: var(--ai-sidebar-bg);
  color: var(--ai-sidebar-text);
}
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  color: var(--ai-sidebar-text);
}
.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--ai-sidebar-hover);
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  color: var(--ai-sidebar-active);
}

.header {
  background: var(--ai-bg-elevated);
  color: var(--ai-text);
  border-bottom: 1px solid var(--ai-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: var(--ai-header-height);
}
.header-left { display: flex; align-items: center; gap: 16px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--ai-text); }
.user-name { font-size: 14px; }
.lang-trigger {
  display: flex; align-items: center; gap: 4px; cursor: pointer;
  padding: 4px 8px; border-radius: 4px; font-size: 14px;
  color: var(--ai-text);
}
.lang-trigger:hover { background: var(--ai-bg-hover); }
.lang-label { font-size: 13px; }

.main {
  padding: 16px;
  background: var(--ai-bg);
  color: var(--ai-text);
  overflow: auto;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
