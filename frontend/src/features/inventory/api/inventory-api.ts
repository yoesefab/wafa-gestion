import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Product } from "@/features/catalog/types"
import type { ManualStockAdjustmentRequest, StockMovement, StockMovementFilters } from "@/features/inventory/types"

export async function listStockMovements(filters: StockMovementFilters) {
  const response = await apiClient.get<PagedResponse<StockMovement>>("/stock-movements", {
    params: { ...filters, sort: "occurredAt,desc" },
  })
  return response.data
}

export async function listInventoryProducts() {
  const products: Product[] = []
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const response = await apiClient.get<PagedResponse<Product>>("/products", {
      params: { page, size: 100, sort: "name,asc" },
    })
    products.push(...response.data.data)
    totalPages = response.data.page.totalPages
    page += 1
  }

  return products
}

export async function createStockAdjustment(productId: number, request: ManualStockAdjustmentRequest) {
  const response = await apiClient.post<ApiEnvelope<StockMovement>>(`/products/${productId}/stock-adjustment`, request)
  return response.data.data
}
