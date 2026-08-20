import { useQuery } from "@tanstack/react-query"

import { getDashboardSales, getDashboardSummary, getLowStockProducts, getRecentSales, getTopProducts } from "@/features/dashboard/api/dashboard-api"

export const dashboardKeys = {
  all: ["dashboard"] as const,
  summary: ["dashboard", "summary"] as const,
  sales: ["dashboard", "sales"] as const,
  topProducts: ["dashboard", "top-products"] as const,
  lowStock: ["dashboard", "low-stock"] as const,
  recentSales: ["dashboard", "recent-sales"] as const,
}

const dashboardQueryOptions = { staleTime: 30_000 }

export const useDashboardSummary = () => useQuery({ queryKey: dashboardKeys.summary, queryFn: getDashboardSummary, ...dashboardQueryOptions })
export const useDashboardSales = () => useQuery({ queryKey: dashboardKeys.sales, queryFn: getDashboardSales, ...dashboardQueryOptions })
export const useTopProducts = () => useQuery({ queryKey: dashboardKeys.topProducts, queryFn: getTopProducts, ...dashboardQueryOptions })
export const useLowStockProducts = () => useQuery({ queryKey: dashboardKeys.lowStock, queryFn: getLowStockProducts, ...dashboardQueryOptions })
export const useRecentSales = () => useQuery({ queryKey: dashboardKeys.recentSales, queryFn: getRecentSales, ...dashboardQueryOptions })
