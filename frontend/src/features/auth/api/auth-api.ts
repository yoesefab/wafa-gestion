import { apiClient } from "@/api/client"
import type { ApiEnvelope } from "@/api/types"
import type { AuthUser, LoginCredentials, LoginResult } from "@/features/auth/types"

export async function loginRequest(credentials: LoginCredentials) {
  const response = await apiClient.post<ApiEnvelope<LoginResult>>("/auth/login", credentials)
  return response.data.data
}

export async function getCurrentUser() {
  const response = await apiClient.get<ApiEnvelope<AuthUser>>("/auth/me")
  return response.data.data
}
