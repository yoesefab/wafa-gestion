export type PartnerKind = "customer" | "supplier"
export type PartnerResource = "customers" | "suppliers"

export interface Partner {
  id: number
  name: string
  ice: string | null
  contactPerson: string | null
  email: string | null
  phone: string | null
  address: string | null
  active: boolean
  deactivatedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface PartnerFilters {
  page: number
  size: number
  search?: string
  active?: boolean
}

export interface PartnerWriteRequest {
  name: string
  ice: string | null
  contactPerson: string | null
  email: string | null
  phone: string | null
  address: string | null
}

export interface PartnerUpdateRequest extends PartnerWriteRequest {
  version: number
}
