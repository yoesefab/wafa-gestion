import { useState } from "react"
import { Eye, FilterX, LoaderCircle, Plus, Search } from "lucide-react"
import { Link } from "react-router-dom"

import { ApiError } from "@/api/types"
import { EmptyState } from "@/components/common/empty-state"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { Pagination } from "@/components/common/pagination"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { usePurchaseOrders } from "@/features/purchases/hooks/use-purchase-orders"
import type { PurchaseOrderStatus, PurchaseOrderSummary } from "@/features/purchases/types"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { formatMoney } from "@/lib/money"

const PAGE_SIZE = 20
const selectClassName = "h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
const dateFormatter = new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" })

function PurchaseStatus({ status }: { status: PurchaseOrderStatus }) {
  const variant = status === "RECEIVED" ? "success" : status === "ORDERED" ? "info" : status === "DRAFT" ? "warning" : "neutral"
  return <StatusBadge variant={variant}>{status[0] + status.slice(1).toLowerCase()}</StatusBadge>
}
function ViewAction({ order }: { order: PurchaseOrderSummary }) {
  return <Button asChild size="sm" variant="ghost"><Link to={`/purchases/${order.id}`}><Eye className="size-4" />View</Link></Button>
}

export function PurchaseOrdersPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState("")
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")
  const debouncedSearch = useDebouncedValue(search.trim())
  const query = usePurchaseOrders({ page, size: PAGE_SIZE, search: debouncedSearch || undefined, status: (status || undefined) as PurchaseOrderStatus | undefined, dateFrom: dateFrom || undefined, dateTo: dateTo || undefined })
  const orders = query.data?.data ?? []
  const hasFilters = Boolean(search || status || dateFrom || dateTo)
  const clearFilters = () => { setSearch(""); setStatus(""); setDateFrom(""); setDateTo(""); setPage(0) }

  return (
    <section className="space-y-6">
      <PageHeader title="Purchase Orders" description="Create drafts and track purchases through ordering and receipt." actions={<Button asChild><Link to="/purchases/new"><Plus className="size-4" />New purchase order</Link></Button>} />
      <div className="rounded-lg border bg-white shadow-sm">
        <div className="grid gap-3 border-b p-4 xl:grid-cols-[minmax(220px,1fr)_170px_160px_160px_auto]">
          <div className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" maxLength={180} placeholder="Search reference or supplier…" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} /></div>
          <select aria-label="Filter by status" className={selectClassName} value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="">All statuses</option><option value="DRAFT">Draft</option><option value="ORDERED">Ordered</option><option value="RECEIVED">Received</option><option value="CANCELLED">Cancelled</option></select>
          <Input aria-label="Orders from date" max={dateTo || undefined} type="date" value={dateFrom} onChange={(event) => { const value = event.target.value; setDateFrom(value); if (dateTo && value > dateTo) setDateTo(""); setPage(0) }} />
          <Input aria-label="Orders to date" min={dateFrom || undefined} type="date" value={dateTo} onChange={(event) => { setDateTo(event.target.value); setPage(0) }} />
          <Button disabled={!hasFilters} onClick={clearFilters} variant="ghost"><FilterX className="size-4" />Clear</Button>
        </div>
        {query.isFetching && !query.isLoading && <div className="flex items-center gap-2 border-b px-4 py-2 text-xs text-muted-foreground"><LoaderCircle className="size-3 animate-spin" />Updating purchase orders…</div>}
        {query.isLoading ? <LoadingSkeleton rows={8} /> : query.isError ? <div className="p-4"><ErrorState message={query.error instanceof ApiError ? query.error.message : "Purchase orders could not be loaded."} onRetry={() => void query.refetch()} /></div> : orders.length === 0 ? <EmptyState title={hasFilters ? "No matching purchase orders" : "No purchase orders yet"} description={hasFilters ? "Try changing or clearing your filters." : "Create a draft purchase order to get started."} action={!hasFilters ? <Button asChild><Link to="/purchases/new">New purchase order</Link></Button> : undefined} /> : (
          <><div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[760px] text-left text-sm"><thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Supplier</th><th className="px-4 py-3">Date</th><th className="px-4 py-3">Status</th><th className="px-4 py-3 text-right">Total</th><th className="px-4 py-3 text-right">Actions</th></tr></thead><tbody className="divide-y">{orders.map((order) => <tr className="hover:bg-slate-50/60" key={order.id}><td className="px-4 py-3 font-mono text-xs font-medium">{order.orderNumber}</td><td className="px-4 py-3 font-medium">{order.party.name}</td><td className="px-4 py-3 text-muted-foreground">{dateFormatter.format(new Date(`${order.orderDate}T00:00:00`))}</td><td className="px-4 py-3"><PurchaseStatus status={order.status} /></td><td className="px-4 py-3 text-right font-medium tabular-nums">{formatMoney(order.totalAmount)}</td><td className="px-4 py-3 text-right"><ViewAction order={order} /></td></tr>)}</tbody></table></div><div className="divide-y md:hidden">{orders.map((order) => <article className="p-4" key={order.id}><div className="flex items-start justify-between gap-3"><div><p className="font-mono text-xs font-medium">{order.orderNumber}</p><p className="mt-1 font-medium">{order.party.name}</p></div><PurchaseStatus status={order.status} /></div><div className="mt-4 flex items-end justify-between"><div><p className="text-xs text-muted-foreground">{dateFormatter.format(new Date(`${order.orderDate}T00:00:00`))}</p><p className="mt-1 font-semibold">{formatMoney(order.totalAmount)}</p></div><ViewAction order={order} /></div></article>)}</div>{query.data?.page && <Pagination page={query.data.page.number} pageSize={query.data.page.size} totalElements={query.data.page.totalElements} totalPages={query.data.page.totalPages} onPageChange={setPage} />}</>
        )}
      </div>
    </section>
  )
}
