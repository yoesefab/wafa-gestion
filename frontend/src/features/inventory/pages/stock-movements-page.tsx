import { useState } from "react"
import { FilterX, LoaderCircle, SlidersHorizontal } from "lucide-react"

import { ApiError } from "@/api/types"
import { EmptyState } from "@/components/common/empty-state"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { Pagination } from "@/components/common/pagination"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { StockAdjustmentDialog } from "@/features/inventory/components/stock-adjustment-dialog"
import { useInventoryProducts, useStockMovements } from "@/features/inventory/hooks/use-inventory"
import type { StockMovement, StockMovementType } from "@/features/inventory/types"

const PAGE_SIZE = 20
const selectClassName = "h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
const dateFormatter = new Intl.DateTimeFormat("en-GB", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Africa/Casablanca",
})

function MovementBadge({ movement }: { movement: StockMovement }) {
  if (movement.movementType === "STOCK_IN") return <StatusBadge variant="success">Stock in</StatusBadge>
  if (movement.movementType === "STOCK_OUT") return <StatusBadge variant="danger">Stock out</StatusBadge>
  if (movement.movementType === "RESTORE") return <StatusBadge variant="info">Restore</StatusBadge>
  return <StatusBadge variant={movement.quantityDelta >= 0 ? "success" : "warning"}>Adjustment {movement.quantityDelta >= 0 ? "in" : "out"}</StatusBadge>
}

function QuantityDelta({ value }: { value: number }) {
  return <span className={value > 0 ? "font-semibold text-emerald-700" : value < 0 ? "font-semibold text-red-700" : "text-muted-foreground"}>{value > 0 ? "+" : ""}{value}</span>
}

function MovementMobileCard({ movement }: { movement: StockMovement }) {
  return (
    <article className="space-y-4 p-4">
      <div className="flex items-start justify-between gap-3">
        <div><p className="font-medium">{movement.product.name}</p><p className="font-mono text-xs text-muted-foreground">{movement.product.sku}</p></div>
        <MovementBadge movement={movement} />
      </div>
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div><p className="text-xs text-muted-foreground">Quantity</p><QuantityDelta value={movement.quantityDelta} /></div>
        <div><p className="text-xs text-muted-foreground">Stock change</p><p className="font-medium tabular-nums">{movement.stockBefore} → {movement.stockAfter}</p></div>
        <div><p className="text-xs text-muted-foreground">Date</p><p>{dateFormatter.format(new Date(movement.occurredAt))}</p></div>
        <div><p className="text-xs text-muted-foreground">Reference</p><p className="break-words font-mono text-xs">{movement.reference || "—"}</p></div>
      </div>
      <div className="text-sm"><p className="text-xs text-muted-foreground">Reason</p><p className="break-words">{movement.reason || "—"}</p></div>
    </article>
  )
}

export function StockMovementsPage() {
  const [page, setPage] = useState(0)
  const [productId, setProductId] = useState("")
  const [movementType, setMovementType] = useState("")
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")
  const [adjusting, setAdjusting] = useState(false)
  const productsQuery = useInventoryProducts()
  const query = useStockMovements({
    page,
    size: PAGE_SIZE,
    productId: productId ? Number(productId) : undefined,
    type: (movementType || undefined) as StockMovementType | undefined,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
  })
  const movements = query.data?.data ?? []
  const hasFilters = Boolean(productId || movementType || dateFrom || dateTo)
  const canAdjust = Boolean(productsQuery.data?.some((product) => product.active))

  function clearFilters() {
    setProductId("")
    setMovementType("")
    setDateFrom("")
    setDateTo("")
    setPage(0)
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="Stock Movements"
        description="Review the backend-authoritative history of every inventory change."
        actions={<Button disabled={productsQuery.isLoading || productsQuery.isError || !canAdjust} onClick={() => setAdjusting(true)}><SlidersHorizontal className="size-4" />Manual adjustment</Button>}
      />
      <div className="rounded-lg border bg-white shadow-sm">
        <div className="grid gap-3 border-b p-4 xl:grid-cols-[minmax(220px,1fr)_190px_160px_160px_auto]">
          <select aria-label="Filter by product" className={selectClassName} disabled={productsQuery.isLoading || productsQuery.isError} value={productId} onChange={(event) => { setProductId(event.target.value); setPage(0) }}>
            <option value="">All products</option>
            {productsQuery.data?.map((product) => <option key={product.id} value={product.id}>{product.sku} — {product.name}{product.active ? "" : " (Archived)"}</option>)}
          </select>
          <select aria-label="Filter by movement type" className={selectClassName} value={movementType} onChange={(event) => { setMovementType(event.target.value); setPage(0) }}>
            <option value="">All movement types</option>
            <option value="STOCK_IN">Stock in</option>
            <option value="STOCK_OUT">Stock out</option>
            <option value="ADJUSTMENT">Manual adjustment</option>
            <option value="RESTORE">Restore</option>
          </select>
          <Input aria-label="Movements from date" max={dateTo || undefined} type="date" value={dateFrom} onChange={(event) => { const value = event.target.value; setDateFrom(value); if (dateTo && value > dateTo) setDateTo(""); setPage(0) }} />
          <Input aria-label="Movements to date" min={dateFrom || undefined} type="date" value={dateTo} onChange={(event) => { setDateTo(event.target.value); setPage(0) }} />
          <Button disabled={!hasFilters} onClick={clearFilters} variant="ghost"><FilterX className="size-4" />Clear</Button>
        </div>
        {productsQuery.isError && <p className="border-b bg-amber-50 px-4 py-2 text-sm text-amber-800">Products could not be loaded. Product filtering and manual adjustments are temporarily unavailable.</p>}
        {query.isFetching && !query.isLoading && <div className="flex items-center gap-2 border-b px-4 py-2 text-xs text-muted-foreground"><LoaderCircle className="size-3 animate-spin" />Updating stock movements…</div>}
        {query.isLoading ? <LoadingSkeleton rows={8} /> : query.isError ? (
          <div className="p-4"><ErrorState message={query.error instanceof ApiError ? query.error.message : "Stock movements could not be loaded."} onRetry={() => void query.refetch()} /></div>
        ) : movements.length === 0 ? (
          <EmptyState title={hasFilters ? "No matching stock movements" : "No stock movements yet"} description={hasFilters ? "Try changing or clearing your filters." : "Inventory movements will appear after stock-changing operations."} />
        ) : (
          <>
            <div className="hidden overflow-x-auto lg:block">
              <table className="w-full min-w-[1080px] text-left text-sm">
                <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Date</th><th className="px-4 py-3">Product</th><th className="px-4 py-3">Movement type</th><th className="px-4 py-3 text-right">Quantity</th><th className="px-4 py-3 text-right">Previous stock</th><th className="px-4 py-3 text-right">New stock</th><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Reason</th></tr></thead>
                <tbody className="divide-y">{movements.map((movement) => <tr className="align-top hover:bg-slate-50/60" key={movement.id}><td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{dateFormatter.format(new Date(movement.occurredAt))}</td><td className="px-4 py-3"><p className="font-medium">{movement.product.name}</p><p className="font-mono text-xs text-muted-foreground">{movement.product.sku}</p></td><td className="px-4 py-3"><MovementBadge movement={movement} /></td><td className="px-4 py-3 text-right tabular-nums"><QuantityDelta value={movement.quantityDelta} /></td><td className="px-4 py-3 text-right tabular-nums">{movement.stockBefore}</td><td className="px-4 py-3 text-right font-medium tabular-nums">{movement.stockAfter}</td><td className="max-w-44 break-words px-4 py-3 font-mono text-xs">{movement.reference || "—"}</td><td className="max-w-64 break-words px-4 py-3">{movement.reason || "—"}</td></tr>)}</tbody>
              </table>
            </div>
            <div className="divide-y lg:hidden">{movements.map((movement) => <MovementMobileCard key={movement.id} movement={movement} />)}</div>
            {query.data?.page && <Pagination page={query.data.page.number} pageSize={query.data.page.size} totalElements={query.data.page.totalElements} totalPages={query.data.page.totalPages} onPageChange={setPage} />}
          </>
        )}
      </div>
      {adjusting && productsQuery.data && <StockAdjustmentDialog onClose={() => setAdjusting(false)} products={productsQuery.data} />}
    </section>
  )
}
