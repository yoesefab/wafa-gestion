export interface DashboardSummary {
  revenueThisMonth: number
  salesOrderCountThisMonth: number
  totalActiveProducts: number
  lowStockProductCount: number
}

export interface MonthlySalesPoint {
  month: string
  orderCount: number
  totalAmount: number
}

export interface DashboardSales {
  year: number
  currency: string
  months: MonthlySalesPoint[]
}

export interface TopProduct {
  product: { id: number; sku: string; name: string }
  quantitySold: number
  revenue: number
}

export interface LowStockProduct {
  id: number
  sku: string
  name: string
  currentStock: number
  minimumStock: number
}
