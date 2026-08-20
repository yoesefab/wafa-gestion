import { createContext } from "react"

import type { AuthUser, LoginCredentials } from "@/features/auth/types"

export interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  isInitializing: boolean
  authNotice: "expired" | "restore-error" | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => void
  clearAuthNotice: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
