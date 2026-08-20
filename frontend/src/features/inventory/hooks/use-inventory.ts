import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { createStockAdjustment, listInventoryProducts, listStockMovements } from "@/features/inventory/api/inventory-api"
import type { ManualStockAdjustmentRequest, StockMovementFilters } from "@/features/inventory/types"

export const inventoryKeys = {
  all: ["stock-movements"] as const,
  list: (filters: StockMovementFilters) => [...inventoryKeys.all, "list", filters] as const,
}

export function useStockMovements(filters: StockMovementFilters) {
  return useQuery({
    queryKey: inventoryKeys.list(filters),
    queryFn: () => listStockMovements(filters),
    placeholderData: keepPreviousData,
  })
}

export function useInventoryProducts() {
  return useQuery({
    queryKey: ["products", "inventory-selector"],
    queryFn: listInventoryProducts,
    staleTime: 60_000,
  })
}

export function useCreateStockAdjustment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, request }: { productId: number; request: ManualStockAdjustmentRequest }) => createStockAdjustment(productId, request),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: inventoryKeys.all }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]),
  })
}
