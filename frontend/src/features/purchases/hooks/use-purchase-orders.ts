import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { createPurchaseOrder, getPurchaseOrder, listPurchaseOrders, listPurchaseProducts, listPurchaseSuppliers, runPurchaseAction, updatePurchaseOrder } from "@/features/purchases/api/purchase-orders-api"
import type { PurchaseAction, PurchaseOrderFilters, PurchaseOrderUpdateRequest } from "@/features/purchases/types"

export const purchaseKeys = {
  all: ["purchase-orders"] as const,
  list: (filters: PurchaseOrderFilters) => [...purchaseKeys.all, "list", filters] as const,
  detail: (id: number) => [...purchaseKeys.all, "detail", id] as const,
}

export function usePurchaseOrders(filters: PurchaseOrderFilters) {
  return useQuery({ queryKey: purchaseKeys.list(filters), queryFn: () => listPurchaseOrders(filters), placeholderData: keepPreviousData })
}
export function usePurchaseOrder(id: number) {
  return useQuery({ queryKey: purchaseKeys.detail(id), queryFn: () => getPurchaseOrder(id), enabled: Number.isInteger(id) && id > 0 })
}
export function usePurchaseSelectors() {
  const suppliers = useQuery({ queryKey: ["suppliers", "purchase-selector"], queryFn: listPurchaseSuppliers, staleTime: 60_000 })
  const products = useQuery({ queryKey: ["products", "purchase-selector"], queryFn: listPurchaseProducts, staleTime: 60_000 })
  return { suppliers, products }
}

function useInvalidatePurchases() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: purchaseKeys.all })
}
export function useCreatePurchaseOrder() {
  const invalidate = useInvalidatePurchases()
  return useMutation({ mutationFn: createPurchaseOrder, onSuccess: invalidate })
}
export function useUpdatePurchaseOrder() {
  const invalidate = useInvalidatePurchases()
  return useMutation({ mutationFn: ({ id, request }: { id: number; request: PurchaseOrderUpdateRequest }) => updatePurchaseOrder(id, request), onSuccess: invalidate, onError: invalidate })
}
export function usePurchaseAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, action }: { id: number; action: PurchaseAction }) => runPurchaseAction(id, action),
    onSuccess: (_data, variables) => variables.action === "receive" ? Promise.all([
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]) : undefined,
    onSettled: () => queryClient.invalidateQueries({ queryKey: purchaseKeys.all }),
  })
}
