export type SalesOrderStatus = "DRAFT" | "CONFIRMED" | "DELIVERED" | "CANCELLED"

export interface SalesParty { id: number; name: string }
export interface SalesProduct { id: number; sku: string; name: string }
export interface SalesActor { id: number; firstName: string; lastName: string; email: string }

export interface SalesOrderSummary {
  id: number
  orderNumber: string
  party: SalesParty
  orderDate: string
  status: SalesOrderStatus
  subtotal: number
  discountAmount: number
  taxAmount: number
  totalAmount: number
  version: number
}

export interface SalesOrderLine {
  id: number
  product: SalesProduct
  quantity: number
  unitPrice: number
  taxRate: number
  lineSubtotal: number
  lineTax: number
  lineTotal: number
}

export interface SalesOrderDetail {
  id: number
  orderNumber: string
  customer: SalesParty
  orderDate: string
  status: SalesOrderStatus
  subtotal: number
  discountAmount: number
  taxAmount: number
  totalAmount: number
  note: string | null
  confirmedAt: string | null
  deliveredAt: string | null
  createdBy: SalesActor
  createdAt: string
  updatedAt: string
  version: number
  lines: SalesOrderLine[]
}

export interface SalesOrderFilters {
  page: number
  size: number
  search?: string
  status?: SalesOrderStatus
  customerId?: number
  dateFrom?: string
  dateTo?: string
}

export interface SalesOrderLineWriteRequest {
  productId: number
  quantity: number
  taxRate: number
}

export interface SalesOrderWriteRequest {
  customerId: number
  orderDate: string
  note: string | null
  lines: SalesOrderLineWriteRequest[]
}

export interface SalesOrderUpdateRequest extends SalesOrderWriteRequest { version: number }
export type SalesAction = "confirm" | "cancel" | "deliver"
