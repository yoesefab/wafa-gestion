import { apiClient } from "@/api/client"
import type { ApiEnvelope, PagedResponse } from "@/api/types"
import type { Partner, PartnerFilters, PartnerResource, PartnerUpdateRequest, PartnerWriteRequest } from "@/features/partners/types"

export async function listPartners(resource: PartnerResource, filters: PartnerFilters) {
  const response = await apiClient.get<PagedResponse<Partner>>(`/${resource}`, { params: { ...filters, search: filters.search || undefined, sort: "name,asc" } })
  return response.data
}

export async function createPartner(resource: PartnerResource, request: PartnerWriteRequest) {
  const response = await apiClient.post<ApiEnvelope<Partner>>(`/${resource}`, request)
  return response.data.data
}

export async function updatePartner(resource: PartnerResource, id: number, request: PartnerUpdateRequest) {
  const response = await apiClient.put<ApiEnvelope<Partner>>(`/${resource}/${id}`, request)
  return response.data.data
}

export async function archivePartner(resource: PartnerResource, id: number, version: number) {
  const response = await apiClient.post<ApiEnvelope<Partner>>(`/${resource}/${id}/archive`, { version })
  return response.data.data
}
