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
        background-color="#001529"
        text-color="rgba(255,255,255,0.85)"
        active-text-color="#409eff"
        router
        class="sidebar-menu"
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
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
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
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { Cpu, Expand, Fold, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activePath = computed(() => route.path)

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
  Connection: 'Connection'
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
    ElMessage.info('个人中心 — Sprint 2 完善')
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
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.logo-icon { font-size: 22px; color: var(--ai-primary); }
.logo-text { font-size: 16px; font-weight: 600; white-space: nowrap; }
.sidebar-menu { border-right: none; }
.sidebar-menu:not(.el-menu--collapse) { width: 200px; }

.header {
  background: #fff;
  border-bottom: 1px solid var(--ai-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: var(--ai-header-height);
}
.header-left { display: flex; align-items: center; gap: 16px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-name { font-size: 14px; }

.main {
  padding: 16px;
  background: var(--ai-bg);
  overflow: auto;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
