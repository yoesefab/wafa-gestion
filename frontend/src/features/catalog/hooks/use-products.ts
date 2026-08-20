import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { archiveProduct, createProduct, listAllCategories, listProducts, updateProduct } from "@/features/catalog/api/products-api"
import type { ProductFilters, ProductUpdateRequest } from "@/features/catalog/types"

export const productKeys = {
  all: ["products"] as const,
  list: (filters: ProductFilters) => [...productKeys.all, "list", filters] as const,
}

const categoryKeys = { selector: ["categories", "selector"] as const }

export function useProducts(filters: ProductFilters) {
  return useQuery({ queryKey: productKeys.list(filters), queryFn: () => listProducts(filters), placeholderData: keepPreviousData })
}

export function useProductCategories() {
  return useQuery({ queryKey: categoryKeys.selector, queryFn: listAllCategories, staleTime: 5 * 60_000 })
}

function useInvalidateProducts() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: productKeys.all })
}

export function useCreateProduct() {
  const invalidate = useInvalidateProducts()
  return useMutation({ mutationFn: createProduct, onSuccess: invalidate })
}

export function useUpdateProduct() {
  const queryClient = useQueryClient()
  const invalidate = () => queryClient.invalidateQueries({ queryKey: productKeys.all })
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: ProductUpdateRequest }) => updateProduct(id, request),
    onSuccess: invalidate,
    onError: invalidate,
  })
}

export function useArchiveProduct() {
  const queryClient = useQueryClient()
  const invalidate = () => queryClient.invalidateQueries({ queryKey: productKeys.all })
  return useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => archiveProduct(id, version), onSuccess: invalidate, onError: invalidate })
}
