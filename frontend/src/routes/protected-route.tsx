import { Navigate, Outlet, useLocation } from "react-router-dom"

import { LoadingState } from "@/components/common/loading-state"
import { useAuth } from "@/features/auth/hooks/use-auth"

export function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()

  if (isInitializing) return <LoadingState fullScreen label="Restoring your session…" />
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  return <Outlet />
}
