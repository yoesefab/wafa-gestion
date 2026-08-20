import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { DashboardSales, DashboardSummary, LowStockProduct, TopProduct } from "@/features/dashboard/types"
import type { SalesOrderSummary } from "@/features/sales/types"

export async function getDashboardSummary() {
  const response = await apiClient.get<ApiEnvelope<DashboardSummary>>("/dashboard/summary")
  return response.data.data
}

export async function getDashboardSales() {
  const response = await apiClient.get<ApiEnvelope<DashboardSales>>("/dashboard/sales")
  return response.data.data
}

export async function getTopProducts() {
  const response = await apiClient.get<ApiEnvelope<TopProduct[]>>("/dashboard/top-products", { params: { limit: 5 } })
  return response.data.data
}

export async function getLowStockProducts() {
  const response = await apiClient.get<ApiEnvelope<LowStockProduct[]>>("/dashboard/low-stock", { params: { limit: 8 } })
  return response.data.data
}

export async function getRecentSales() {
  const response = await apiClient.get<PagedResponse<SalesOrderSummary>>("/sales-orders", {
    params: { page: 0, size: 5, sort: "orderDate,desc" },
  })
  return response.data.data
}
