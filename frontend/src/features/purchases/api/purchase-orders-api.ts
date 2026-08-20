import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Product } from "@/features/catalog/types"
import type { Partner } from "@/features/partners/types"
import type { PurchaseAction, PurchaseOrderDetail, PurchaseOrderFilters, PurchaseOrderSummary, PurchaseOrderUpdateRequest, PurchaseOrderWriteRequest } from "@/features/purchases/types"

export async function listPurchaseOrders(filters: PurchaseOrderFilters) {
  const response = await apiClient.get<PagedResponse<PurchaseOrderSummary>>("/purchase-orders", { params: { ...filters, search: filters.search || undefined, sort: "orderDate,desc" } })
  return response.data
}

export async function getPurchaseOrder(id: number) {
  const response = await apiClient.get<ApiEnvelope<PurchaseOrderDetail>>(`/purchase-orders/${id}`)
  return response.data.data
}

export async function createPurchaseOrder(request: PurchaseOrderWriteRequest) {
  const response = await apiClient.post<ApiEnvelope<PurchaseOrderDetail>>("/purchase-orders", request)
  return response.data.data
}

export async function updatePurchaseOrder(id: number, request: PurchaseOrderUpdateRequest) {
  const response = await apiClient.put<ApiEnvelope<PurchaseOrderDetail>>(`/purchase-orders/${id}`, request)
  return response.data.data
}

export async function runPurchaseAction(id: number, action: PurchaseAction) {
  const response = await apiClient.post<ApiEnvelope<PurchaseOrderDetail>>(`/purchase-orders/${id}/${action}`)
  return response.data.data
}

async function collectPages<T>(path: string) {
  const values: T[] = []
  let page = 0
  let totalPages = 1
  while (page < totalPages) {
    const response = await apiClient.get<PagedResponse<T>>(path, { params: { page, size: 100, active: true, sort: "name,asc" } })
    values.push(...response.data.data)
    totalPages = response.data.page.totalPages
    page += 1
  }
  return values
}

export const listPurchaseProducts = () => collectPages<Product>("/products")
export const listPurchaseSuppliers = () => collectPages<Partner>("/suppliers")
