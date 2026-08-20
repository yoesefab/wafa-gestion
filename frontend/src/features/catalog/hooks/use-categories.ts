import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { archiveCategory, createCategory, listCategories, updateCategory, type CategoryFilters, type CategoryUpdateRequest } from "@/features/catalog/api/categories-api"

export const categoryKeys = {
  all: ["categories"] as const,
  list: (filters: CategoryFilters) => [...categoryKeys.all, "list", filters] as const,
}

export function useCategories(filters: CategoryFilters) {
  return useQuery({ queryKey: categoryKeys.list(filters), queryFn: () => listCategories(filters), placeholderData: keepPreviousData })
}

function useInvalidateCategories() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: categoryKeys.all })
}

export function useCreateCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({ mutationFn: createCategory, onSuccess: invalidate })
}

export function useUpdateCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({ mutationFn: ({ id, request }: { id: number; request: CategoryUpdateRequest }) => updateCategory(id, request), onSuccess: invalidate, onError: invalidate })
}

export function useArchiveCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => archiveCategory(id, version), onSuccess: invalidate, onError: invalidate })
}
