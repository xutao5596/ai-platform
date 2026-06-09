import { post, get } from '@/utils/http'
import type { LoginRequest, LoginResponse, UserInfo } from '@/api/auth'

export const authApi = {
  login: (data: LoginRequest) => post<LoginResponse>('/v1/auth/login', data),
  logout: () => post('/v1/auth/logout'),
  refresh: (refreshToken: string) => post<LoginResponse>('/v1/auth/refresh', { refreshToken }),
  profile: () => get<UserInfo>('/v1/auth/profile'),
  myMenus: () => get<any[]>('/v1/system/menu/mine'),
  myPermissions: () => get<string[]>('/v1/system/menu/permissions')
}
