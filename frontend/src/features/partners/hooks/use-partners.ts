import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { archivePartner, createPartner, listPartners, updatePartner } from "@/features/partners/api/partners-api"
import type { PartnerFilters, PartnerResource, PartnerUpdateRequest, PartnerWriteRequest } from "@/features/partners/types"

const partnerKeys = {
  all: (resource: PartnerResource) => [resource] as const,
  list: (resource: PartnerResource, filters: PartnerFilters) => [...partnerKeys.all(resource), "list", filters] as const,
}

export function usePartners(resource: PartnerResource, filters: PartnerFilters) {
  return useQuery({ queryKey: partnerKeys.list(resource, filters), queryFn: () => listPartners(resource, filters), placeholderData: keepPreviousData })
}

function useInvalidatePartners(resource: PartnerResource) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: partnerKeys.all(resource) })
}

export function useCreatePartner(resource: PartnerResource) {
  const invalidate = useInvalidatePartners(resource)
  return useMutation({ mutationFn: (request: PartnerWriteRequest) => createPartner(resource, request), onSuccess: invalidate })
}

export function useUpdatePartner(resource: PartnerResource) {
  const invalidate = useInvalidatePartners(resource)
  return useMutation({ mutationFn: ({ id, request }: { id: number; request: PartnerUpdateRequest }) => updatePartner(resource, id, request), onSuccess: invalidate, onError: invalidate })
}

export function useArchivePartner(resource: PartnerResource) {
  const invalidate = useInvalidatePartners(resource)
  return useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => archivePartner(resource, id, version), onSuccess: invalidate, onError: invalidate })
}
