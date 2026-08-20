import type { ReactNode } from "react"
import { ArrowRight, Package, ShoppingCart, TrendingUp, TriangleAlert, type LucideIcon } from "lucide-react"
import { Link } from "react-router-dom"

import { ApiError } from "@/api/types"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { Button } from "@/components/ui/button"
import { MonthlyRevenueChart, TopProductsChart } from "@/features/dashboard/components/dashboard-charts"
import { LowStockTable, RecentSalesTable } from "@/features/dashboard/components/dashboard-tables"
import { useDashboardSales, useDashboardSummary, useLowStockProducts, useRecentSales, useTopProducts } from "@/features/dashboard/hooks/use-dashboard"
import { formatMoney } from "@/lib/money"

const countFormatter = new Intl.NumberFormat("en")

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function SummaryCard({ title, value, detail, icon: Icon, attention }: { title: string; value: string; detail: string; icon: LucideIcon; attention?: boolean }) {
  return (
    <article className="rounded-lg border bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div><p className="text-sm font-medium text-muted-foreground">{title}</p><p className={attention ? "mt-2 text-2xl font-semibold tracking-tight text-amber-700" : "mt-2 text-2xl font-semibold tracking-tight"}>{value}</p></div>
        <div className={attention ? "grid size-10 place-items-center rounded-lg bg-amber-50 text-amber-700" : "grid size-10 place-items-center rounded-lg bg-slate-100 text-slate-700"}><Icon className="size-5" /></div>
      </div>
      <p className="mt-3 text-xs text-muted-foreground">{detail}</p>
    </article>
  )
}

function SummarySkeleton() {
  return <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Loading dashboard summary" role="status">{Array.from({ length: 4 }, (_, index) => <div className="h-32 animate-pulse rounded-lg border bg-white p-5 shadow-sm" key={index}><div className="h-4 w-28 rounded bg-slate-100" /><div className="mt-4 h-8 w-24 rounded bg-slate-100" /><div className="mt-4 h-3 w-36 rounded bg-slate-100" /></div>)}</div>
}

function Panel({ title, description, action, children }: { title: string; description?: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="overflow-hidden rounded-lg border bg-white shadow-sm">
      <header className="flex items-start justify-between gap-4 border-b px-5 py-4">
        <div><h2 className="font-semibold">{title}</h2>{description && <p className="mt-1 text-xs text-muted-foreground">{description}</p>}</div>
        {action}
      </header>
      {children}
    </section>
  )
}

export function DashboardPage() {
  const summary = useDashboardSummary()
  const sales = useDashboardSales()
  const topProducts = useTopProducts()
  const lowStock = useLowStockProducts()
  const recentSales = useRecentSales()

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="A concise view of sales performance and current inventory attention points." />

      {summary.isLoading ? <SummarySkeleton /> : summary.isError ? (
        <ErrorState message={errorMessage(summary.error, "The dashboard summary could not be loaded.")} onRetry={() => void summary.refetch()} />
      ) : summary.data && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryCard detail="Confirmed and delivered sales" icon={TrendingUp} title="Revenue this month" value={formatMoney(summary.data.revenueThisMonth)} />
          <SummaryCard detail="Confirmed and delivered orders" icon={ShoppingCart} title="Sales orders this month" value={countFormatter.format(summary.data.salesOrderCountThisMonth)} />
          <SummaryCard detail="Active catalog products" icon={Package} title="Products" value={countFormatter.format(summary.data.totalActiveProducts)} />
          <SummaryCard attention={summary.data.lowStockProductCount > 0} detail="At or below minimum stock" icon={TriangleAlert} title="Low-stock products" value={countFormatter.format(summary.data.lowStockProductCount)} />
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        <Panel title="Monthly revenue" description={sales.data ? `${sales.data.year} · ${sales.data.currency}` : "Confirmed and delivered sales"}>
          {sales.isLoading ? <LoadingSkeleton rows={5} /> : sales.isError ? <div className="p-4"><ErrorState message={errorMessage(sales.error, "Monthly revenue could not be loaded.")} onRetry={() => void sales.refetch()} /></div> : sales.data && <div className="p-4"><MonthlyRevenueChart data={sales.data.months} /></div>}
        </Panel>
        <Panel title="Top-selling products" description="Current month, ranked by quantity sold">
          {topProducts.isLoading ? <LoadingSkeleton rows={5} /> : topProducts.isError ? <div className="p-4"><ErrorState message={errorMessage(topProducts.error, "Top-selling products could not be loaded.")} onRetry={() => void topProducts.refetch()} /></div> : topProducts.data && <div className="p-4"><TopProductsChart data={topProducts.data} /></div>}
        </Panel>
      </div>

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1.45fr)_minmax(380px,0.75fr)]">
        <Panel title="Recent sales" description="Latest sales orders across all statuses" action={<Button asChild size="sm" variant="ghost"><Link to="/sales">View all<ArrowRight className="size-4" /></Link></Button>}>
          {recentSales.isLoading ? <LoadingSkeleton rows={5} /> : recentSales.isError ? <div className="p-4"><ErrorState message={errorMessage(recentSales.error, "Recent sales could not be loaded.")} onRetry={() => void recentSales.refetch()} /></div> : recentSales.data && <RecentSalesTable sales={recentSales.data} />}
        </Panel>
        <Panel title="Low-stock products" description="Active products requiring attention" action={<Button asChild size="sm" variant="ghost"><Link to="/products">Products<ArrowRight className="size-4" /></Link></Button>}>
          {lowStock.isLoading ? <LoadingSkeleton rows={5} /> : lowStock.isError ? <div className="p-4"><ErrorState message={errorMessage(lowStock.error, "Low-stock products could not be loaded.")} onRetry={() => void lowStock.refetch()} /></div> : lowStock.data && <LowStockTable products={lowStock.data} />}
        </Panel>
      </div>
    </div>
  )
}
