export type UnitOfMeasure = "PIECE" | "BOX" | "PACK"

export interface Category {
  id: number
  name: string
  description: string | null
  active: boolean
  deactivatedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface Product {
  id: number
  sku: string
  name: string
  category: { id: number; name: string }
  unitOfMeasure: UnitOfMeasure
  purchasePrice: number
  sellingPrice: number
  currentStock: number
  minimumStock: number
  lowStock: boolean
  active: boolean
  deactivatedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface ProductFilters {
  page: number
  size: number
  search?: string
  categoryId?: number
  active?: boolean
  lowStock?: boolean
}

export interface ProductWriteRequest {
  sku: string
  name: string
  categoryId: number
  unitOfMeasure: UnitOfMeasure
  purchasePrice: number
  sellingPrice: number
  minimumStock: number
}

export interface ProductUpdateRequest extends ProductWriteRequest {
  version: number
}
