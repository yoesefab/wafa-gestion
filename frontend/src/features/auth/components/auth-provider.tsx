import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"

import { AUTH_EXPIRED_EVENT } from "@/api/client"
import { ApiError } from "@/api/types"
import { tokenStore } from "@/api/token-store"
import { queryClient } from "@/lib/query-client"
import { getCurrentUser, loginRequest } from "@/features/auth/api/auth-api"
import { AuthContext } from "@/features/auth/auth-context"
import type { AuthUser, LoginCredentials } from "@/features/auth/types"

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isInitializing, setIsInitializing] = useState(true)
  const [authNotice, setAuthNotice] = useState<"expired" | "restore-error" | null>(null)

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setAuthNotice(null)
    queryClient.clear()
  }, [])

  const clearAuthNotice = useCallback(() => setAuthNotice(null), [])

  useEffect(() => {
    const handleExpired = () => {
      tokenStore.clear()
      setUser(null)
      setAuthNotice("expired")
      queryClient.clear()
    }
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired)

    const restoreSession = async () => {
      if (!tokenStore.get()) {
        setIsInitializing(false)
        return
      }

      try {
        setUser(await getCurrentUser())
      } catch (error) {
        tokenStore.clear()
        setUser(null)
        if (!(error instanceof ApiError && error.status === 401)) {
          setAuthNotice("restore-error")
        }
      } finally {
        setIsInitializing(false)
      }
    }

    void restoreSession()
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired)
  }, [])

  const login = useCallback(async (credentials: LoginCredentials) => {
    setAuthNotice(null)
    const result = await loginRequest(credentials)
    tokenStore.set(result.accessToken)
    try {
      setUser(await getCurrentUser())
    } catch (error) {
      tokenStore.clear()
      throw error
    }
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated: user !== null, isInitializing, authNotice, login, logout, clearAuthNotice }),
    [user, isInitializing, authNotice, login, logout, clearAuthNotice],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
