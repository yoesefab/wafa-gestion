import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Category } from "@/features/catalog/types"

export interface CategoryFilters {
  page: number
  size: number
  search?: string
  active?: boolean
}

export interface CategoryWriteRequest {
  name: string
  description: string | null
}

export interface CategoryUpdateRequest extends CategoryWriteRequest {
  version: number
}

export async function listCategories(filters: CategoryFilters) {
  const response = await apiClient.get<PagedResponse<Category>>("/categories", { params: { ...filters, search: filters.search || undefined, sort: "name,asc" } })
  return response.data
}

export async function createCategory(request: CategoryWriteRequest) {
  const response = await apiClient.post<ApiEnvelope<Category>>("/categories", request)
  return response.data.data
}

export async function updateCategory(id: number, request: CategoryUpdateRequest) {
  const response = await apiClient.put<ApiEnvelope<Category>>(`/categories/${id}`, request)
  return response.data.data
}

export async function archiveCategory(id: number, version: number) {
  const response = await apiClient.post<ApiEnvelope<Category>>(`/categories/${id}/archive`, { version })
  return response.data.data
}
