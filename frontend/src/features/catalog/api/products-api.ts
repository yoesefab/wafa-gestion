import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Category, Product, ProductFilters, ProductUpdateRequest, ProductWriteRequest } from "@/features/catalog/types"

export async function listProducts(filters: ProductFilters) {
  const response = await apiClient.get<PagedResponse<Product>>("/products", {
    params: { ...filters, search: filters.search || undefined, sort: "name,asc" },
  })
  return response.data
}

export async function listAllCategories() {
  const categories: Category[] = []
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const response = await apiClient.get<PagedResponse<Category>>("/categories", {
      params: { page, size: 100, sort: "name,asc" },
    })
    categories.push(...response.data.data)
    totalPages = response.data.page.totalPages
    page += 1
  }

  return categories
}

export async function createProduct(request: ProductWriteRequest) {
  const response = await apiClient.post<ApiEnvelope<Product>>("/products", request)
  return response.data.data
}

export async function updateProduct(id: number, request: ProductUpdateRequest) {
  const response = await apiClient.put<ApiEnvelope<Product>>(`/products/${id}`, request)
  return response.data.data
}

export async function archiveProduct(id: number, version: number) {
  const response = await apiClient.post<ApiEnvelope<Product>>(`/products/${id}/archive`, { version })
  return response.data.data
}
