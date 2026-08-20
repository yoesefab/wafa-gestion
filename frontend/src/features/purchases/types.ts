export type PurchaseOrderStatus = "DRAFT" | "ORDERED" | "RECEIVED" | "CANCELLED"
export interface PurchaseParty { id: number; name: string }
export interface PurchaseProduct { id: number; sku: string; name: string }
export interface PurchaseActor { id: number; firstName: string; lastName: string; email: string }

export interface PurchaseOrderSummary {
  id: number; orderNumber: string; party: PurchaseParty; orderDate: string; status: PurchaseOrderStatus
  subtotal: number; discountAmount: number; taxAmount: number; totalAmount: number; version: number
}

export interface PurchaseOrderLine {
  id: number; product: PurchaseProduct; quantity: number; unitPrice: number; taxRate: number
  lineSubtotal: number; lineTax: number; lineTotal: number
}

export interface PurchaseOrderDetail {
  id: number; orderNumber: string; supplier: PurchaseParty; orderDate: string; status: PurchaseOrderStatus
  subtotal: number; discountAmount: number; taxAmount: number; totalAmount: number; note: string | null
  orderedAt: string | null; receivedAt: string | null; createdBy: PurchaseActor; createdAt: string; updatedAt: string
  version: number; lines: PurchaseOrderLine[]
}

export interface PurchaseOrderFilters {
  page: number; size: number; search?: string; status?: PurchaseOrderStatus; supplierId?: number; dateFrom?: string; dateTo?: string
}

export interface PurchaseOrderLineWriteRequest { productId: number; quantity: number; unitPrice: number; taxRate: number }
export interface PurchaseOrderWriteRequest { supplierId: number; orderDate: string; note: string | null; lines: PurchaseOrderLineWriteRequest[] }
export interface PurchaseOrderUpdateRequest extends PurchaseOrderWriteRequest { version: number }
export type PurchaseAction = "order" | "receive" | "cancel"
