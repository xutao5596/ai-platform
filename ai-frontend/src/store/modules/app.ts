import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { MenuNode } from '@/api/system/menu'

interface AppState {
  sidebarCollapsed: boolean
  menus: MenuNode[]
  currentProjectId: number | null
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    sidebarCollapsed: false,
    menus: [],
    currentProjectId: null
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    async fetchMenus() {
      this.menus = await authApi.myMenus()
    },
    setCurrentProject(id: number | null) {
      this.currentProjectId = id
    }
  },
  persist: {
    key: 'ai-platform-app',
    storage: localStorage
  }
})
