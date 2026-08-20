import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { createSalesOrder, downloadSalesInvoice, getSalesOrder, listSalesCustomers, listSalesOrders, listSalesProducts, runSalesAction, updateSalesOrder } from "@/features/sales/api/sales-orders-api"
import type { SalesAction, SalesOrderFilters, SalesOrderUpdateRequest } from "@/features/sales/types"

export const salesKeys = {
  all: ["sales-orders"] as const,
  list: (filters: SalesOrderFilters) => [...salesKeys.all, "list", filters] as const,
  detail: (id: number) => [...salesKeys.all, "detail", id] as const,
}

export function useSalesOrders(filters: SalesOrderFilters) {
  return useQuery({ queryKey: salesKeys.list(filters), queryFn: () => listSalesOrders(filters), placeholderData: keepPreviousData })
}

export function useSalesOrder(id: number) {
  return useQuery({ queryKey: salesKeys.detail(id), queryFn: () => getSalesOrder(id), enabled: Number.isInteger(id) && id > 0 })
}

export function useSalesSelectors() {
  const customers = useQuery({ queryKey: ["customers", "sales-selector"], queryFn: listSalesCustomers, staleTime: 60_000 })
  const products = useQuery({ queryKey: ["products", "sales-selector"], queryFn: listSalesProducts, staleTime: 60_000 })
  return { customers, products }
}

function useInvalidateSales() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: salesKeys.all })
}

export function useCreateSalesOrder() {
  const invalidate = useInvalidateSales()
  return useMutation({ mutationFn: createSalesOrder, onSuccess: invalidate })
}

export function useUpdateSalesOrder() {
  const invalidate = useInvalidateSales()
  return useMutation({ mutationFn: ({ id, request }: { id: number; request: SalesOrderUpdateRequest }) => updateSalesOrder(id, request), onSuccess: invalidate, onError: invalidate })
}

export function useSalesAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, action }: { id: number; action: SalesAction }) => runSalesAction(id, action),
    onSettled: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: salesKeys.all }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]),
  })
}

export function useSalesInvoice() {
  return useMutation({ mutationFn: downloadSalesInvoice })
}
