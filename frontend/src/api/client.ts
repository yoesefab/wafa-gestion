import axios, { AxiosError } from "axios"

import { ApiError, type ApiProblem } from "@/api/types"
import { tokenStore } from "@/api/token-store"

export const AUTH_EXPIRED_EVENT = "wafa:auth-expired"

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 15_000,
  headers: { Accept: "application/json", "Content-Type": "application/json" },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiProblem>) => {
    const status = error.response?.status ?? 0
    if (status === 401 && !error.config?.url?.endsWith("/auth/login")) {
      tokenStore.clear()
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }

    const problem = error.response?.data
    if (problem && typeof problem === "object") {
      return Promise.reject(
        new ApiError({
          title: problem.title ?? "Request failed",
          detail: problem.detail ?? "The request could not be completed.",
          status,
          code: problem.code,
          fieldErrors: problem.fieldErrors ?? [],
          traceId: problem.traceId,
        }),
      )
    }

    return Promise.reject(
      new ApiError({
        title: "Connection error",
        detail: status ? "The server returned an unexpected response." : "Unable to reach the server. Please try again.",
        status,
        fieldErrors: [],
      }),
    )
  },
)
