export type StockMovementType = "STOCK_IN" | "STOCK_OUT" | "ADJUSTMENT" | "RESTORE"
export type AdjustmentDirection = "IN" | "OUT"

export interface StockMovement {
  id: number
  product: { id: number; sku: string; name: string }
  movementType: StockMovementType
  quantityDelta: number
  stockBefore: number
  stockAfter: number
  reference: string
  reason: string
  note: string | null
  createdBy: { id: number; firstName: string; lastName: string; email: string }
  occurredAt: string
}

export interface StockMovementFilters {
  page: number
  size: number
  productId?: number
  type?: StockMovementType
  dateFrom?: string
  dateTo?: string
}

export interface ManualStockAdjustmentRequest {
  direction: AdjustmentDirection
  quantity: number
  reference: string
  reason: string
  note?: string
}
