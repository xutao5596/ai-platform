import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginRequest, UserInfo } from '@/types/auth'

interface UserState {
  token: string
  refreshToken: string
  userInfo: UserInfo | null
  permissions: string[]
  roles: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    refreshToken: '',
    userInfo: null,
    permissions: [],
    roles: []
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => !!s.userInfo?.admin,
    realName: (s) => s.userInfo?.realName || s.userInfo?.username || '未登录'
  },
  actions: {
    async login(payload: LoginRequest) {
      const res = await authApi.login(payload)
      this.token = res.accessToken
      this.refreshToken = res.refreshToken
      this.userInfo = res.user
      this.roles = res.user.roles || []
      this.permissions = res.user.permissions || []
    },
    async fetchProfile() {
      const u = await authApi.profile()
      this.userInfo = u
      this.roles = u.roles || []
      this.permissions = u.permissions || []
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // ignore
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
    },
    hasPermission(perm: string) {
      if (this.isAdmin) return true
      return this.permissions.includes(perm)
    }
  },
  persist: {
    key: 'ai-platform-user',
    storage: localStorage
  }
})
