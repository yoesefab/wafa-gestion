import { Eye } from "lucide-react"
import { Link } from "react-router-dom"

import { EmptyState } from "@/components/common/empty-state"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import type { LowStockProduct } from "@/features/dashboard/types"
import type { SalesOrderStatus, SalesOrderSummary } from "@/features/sales/types"
import { formatMoney } from "@/lib/money"

const dateFormatter = new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" })

function SalesStatus({ status }: { status: SalesOrderStatus }) {
  const variant = status === "DELIVERED" ? "success" : status === "CONFIRMED" ? "info" : status === "DRAFT" ? "warning" : "neutral"
  return <StatusBadge variant={variant}>{status[0] + status.slice(1).toLowerCase()}</StatusBadge>
}

export function RecentSalesTable({ sales }: { sales: SalesOrderSummary[] }) {
  if (sales.length === 0) return <EmptyState title="No sales orders yet" description="Recent sales orders will appear here." />
  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[620px] text-left text-sm">
          <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Customer</th><th className="px-4 py-3">Date</th><th className="px-4 py-3">Status</th><th className="px-4 py-3 text-right">Total</th><th className="px-4 py-3"><span className="sr-only">Actions</span></th></tr></thead>
          <tbody className="divide-y">{sales.map((sale) => <tr className="hover:bg-slate-50/60" key={sale.id}><td className="px-4 py-3 font-mono text-xs font-medium">{sale.orderNumber}</td><td className="px-4 py-3 font-medium">{sale.party.name}</td><td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{dateFormatter.format(new Date(`${sale.orderDate}T00:00:00`))}</td><td className="px-4 py-3"><SalesStatus status={sale.status} /></td><td className="px-4 py-3 text-right font-medium tabular-nums">{formatMoney(sale.totalAmount)}</td><td className="px-4 py-3 text-right"><Button asChild size="sm" variant="ghost"><Link aria-label={`View ${sale.orderNumber}`} to={`/sales/${sale.id}`}><Eye className="size-4" /></Link></Button></td></tr>)}</tbody>
        </table>
      </div>
      <div className="divide-y md:hidden">{sales.map((sale) => <article className="p-4" key={sale.id}><div className="flex items-start justify-between gap-3"><div><p className="font-mono text-xs font-medium">{sale.orderNumber}</p><p className="mt-1 font-medium">{sale.party.name}</p></div><SalesStatus status={sale.status} /></div><div className="mt-4 flex items-end justify-between"><div><p className="text-xs text-muted-foreground">{dateFormatter.format(new Date(`${sale.orderDate}T00:00:00`))}</p><p className="mt-1 font-semibold">{formatMoney(sale.totalAmount)}</p></div><Button asChild size="sm" variant="ghost"><Link to={`/sales/${sale.id}`}><Eye className="size-4" />View</Link></Button></div></article>)}</div>
    </>
  )
}

export function LowStockTable({ products }: { products: LowStockProduct[] }) {
  if (products.length === 0) return <EmptyState title="Stock levels look healthy" description="No active products are currently at or below minimum stock." />
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[420px] text-left text-sm">
        <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Product</th><th className="px-4 py-3 text-right">Stock</th><th className="px-4 py-3 text-right">Minimum</th><th className="px-4 py-3">Status</th></tr></thead>
        <tbody className="divide-y">{products.map((product) => <tr key={product.id}><td className="px-4 py-3"><p className="font-medium">{product.name}</p><p className="font-mono text-xs text-muted-foreground">{product.sku}</p></td><td className="px-4 py-3 text-right font-semibold tabular-nums">{product.currentStock}</td><td className="px-4 py-3 text-right tabular-nums text-muted-foreground">{product.minimumStock}</td><td className="px-4 py-3"><StatusBadge variant={product.currentStock === 0 ? "danger" : "warning"}>{product.currentStock === 0 ? "Out of stock" : "Low stock"}</StatusBadge></td></tr>)}</tbody>
      </table>
    </div>
  )
}
