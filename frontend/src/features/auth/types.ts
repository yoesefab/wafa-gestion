export interface AuthUser {
  id: number
  firstName: string
  lastName: string
  email: string
  active: boolean
  createdAt: string
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: "Bearer"
  expiresAt: string
  user: AuthUser
}
