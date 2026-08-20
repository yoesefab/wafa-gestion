import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Product } from "@/features/catalog/types"
import type { Partner } from "@/features/partners/types"
import type { SalesAction, SalesOrderDetail, SalesOrderFilters, SalesOrderSummary, SalesOrderUpdateRequest, SalesOrderWriteRequest } from "@/features/sales/types"

export async function listSalesOrders(filters: SalesOrderFilters) {
  const response = await apiClient.get<PagedResponse<SalesOrderSummary>>("/sales-orders", { params: { ...filters, search: filters.search || undefined, sort: "orderDate,desc" } })
  return response.data
}

export async function getSalesOrder(id: number) {
  const response = await apiClient.get<ApiEnvelope<SalesOrderDetail>>(`/sales-orders/${id}`)
  return response.data.data
}

export async function createSalesOrder(request: SalesOrderWriteRequest) {
  const response = await apiClient.post<ApiEnvelope<SalesOrderDetail>>("/sales-orders", request)
  return response.data.data
}

export async function updateSalesOrder(id: number, request: SalesOrderUpdateRequest) {
  const response = await apiClient.put<ApiEnvelope<SalesOrderDetail>>(`/sales-orders/${id}`, request)
  return response.data.data
}

export async function runSalesAction(id: number, action: SalesAction) {
  const response = await apiClient.post<ApiEnvelope<SalesOrderDetail>>(`/sales-orders/${id}/${action}`)
  return response.data.data
}

export async function downloadSalesInvoice(id: number) {
  const response = await apiClient.get<Blob>(`/sales-orders/${id}/invoice`, { responseType: "blob" })
  const disposition = response.headers["content-disposition"] as string | undefined
  const encodedFilename = disposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  const quotedFilename = disposition?.match(/filename="([^"]+)"/i)?.[1]
  return {
    blob: response.data,
    filename: encodedFilename ? decodeURIComponent(encodedFilename) : quotedFilename ?? `sales-order-${id}.pdf`,
  }
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

export const listSalesProducts = () => collectPages<Product>("/products")
export const listSalesCustomers = () => collectPages<Partner>("/customers")
