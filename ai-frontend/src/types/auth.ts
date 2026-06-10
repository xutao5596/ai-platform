export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  realName?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  deptName?: string
  roles?: string[]
  permissions?: string[]
  admin?: boolean
  status?: number
}
