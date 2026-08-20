import { useState } from "react"
import { ArrowLeft, Ban, CheckCircle2, PackageCheck, Pencil } from "lucide-react"
import { Link, useParams } from "react-router-dom"

import { ConfirmDialog } from "@/components/common/confirm-dialog"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import { PurchaseOrderForm } from "@/features/purchases/components/purchase-order-form"
import { usePurchaseAction, usePurchaseOrder } from "@/features/purchases/hooks/use-purchase-orders"
import { purchaseErrorMessage } from "@/features/purchases/lib/errors"
import type { PurchaseAction, PurchaseOrderStatus } from "@/features/purchases/types"
import { formatMoney } from "@/lib/money"
import { notifications } from "@/lib/notifications"

const dateFormatter = new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" })
const timestampFormatter = new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Casablanca" })

function PurchaseStatus({ status }: { status: PurchaseOrderStatus }) {
  const variant = status === "RECEIVED" ? "success" : status === "ORDERED" ? "info" : status === "DRAFT" ? "warning" : "neutral"
  return <StatusBadge variant={variant}>{status[0] + status.slice(1).toLowerCase()}</StatusBadge>
}

const actionCopy: Record<PurchaseAction, { title: string; confirmLabel: string; description: string; success: string }> = {
  order: { title: "Mark purchase as ordered", confirmLabel: "Mark ordered", description: "Mark this draft as ordered? Its supplier, date, and product lines will become read-only.", success: "Purchase order marked as ordered" },
  receive: { title: "Receive purchase order", confirmLabel: "Receive and update inventory", description: "Receiving this purchase increases inventory for every product line and creates permanent stock movements. Continue?", success: "Purchase received and inventory updated" },
  cancel: { title: "Cancel purchase order", confirmLabel: "Cancel order", description: "Cancel this purchase order? It will become read-only and cannot be restored. Inventory will not change.", success: "Purchase order cancelled" },
}

export function PurchaseOrderDetailPage() {
  const id = Number(useParams().id)
  const query = usePurchaseOrder(id)
  const actionMutation = usePurchaseAction()
  const [editing, setEditing] = useState(false)
  const [pendingAction, setPendingAction] = useState<PurchaseAction | null>(null)
  const [actionError, setActionError] = useState("")
  const [successMessage, setSuccessMessage] = useState("")

  if (!Number.isInteger(id) || id <= 0) return <ErrorState title="Invalid purchase order" message="The purchase order identifier is invalid." />
  if (query.isLoading) return <div className="rounded-lg border bg-white"><LoadingSkeleton rows={9} /></div>
  if (query.isError || !query.data) return <ErrorState message={purchaseErrorMessage(query.error)} onRetry={() => void query.refetch()} />
  const order = query.data

  async function runAction() {
    if (!pendingAction) return
    const action = pendingAction
    const copy = actionCopy[action]
    setActionError("")
    setSuccessMessage("")
    try {
      await actionMutation.mutateAsync({ id: order.id, action })
      notifications.success(copy.success, order.orderNumber)
      if (action === "receive") setSuccessMessage("Purchase received successfully. Product inventory and stock-movement history have been updated.")
      setPendingAction(null)
      await query.refetch()
    } catch (error) {
      setActionError(purchaseErrorMessage(error))
      setPendingAction(null)
    }
  }

  if (editing) return <section className="space-y-6"><PageHeader title={`Edit ${order.orderNumber}`} description="Only draft purchase orders can be edited. Saved totals are recalculated by the backend." actions={<Button onClick={() => setEditing(false)} variant="outline"><ArrowLeft className="size-4" />Back to details</Button>} /><PurchaseOrderForm onSaved={() => { setEditing(false); void query.refetch() }} order={order} /></section>

  return (
    <section className="space-y-6">
      <PageHeader
        title={order.orderNumber}
        description={`${order.supplier.name} · ${dateFormatter.format(new Date(`${order.orderDate}T00:00:00`))}`}
        actions={<div className="flex flex-wrap gap-2"><Button asChild variant="outline"><Link to="/purchases"><ArrowLeft className="size-4" />Back</Link></Button>{order.status === "DRAFT" && <><Button onClick={() => setEditing(true)} variant="outline"><Pencil className="size-4" />Edit</Button><Button onClick={() => setPendingAction("cancel")} variant="outline"><Ban className="size-4" />Cancel</Button><Button onClick={() => setPendingAction("order")}><CheckCircle2 className="size-4" />Mark ordered</Button></>}{order.status === "ORDERED" && <><Button onClick={() => setPendingAction("cancel")} variant="outline"><Ban className="size-4" />Cancel</Button><Button onClick={() => setPendingAction("receive")}><PackageCheck className="size-4" />Receive</Button></>}</div>}
      />
      {successMessage && <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">{successMessage}</div>}
      {actionError && <div className="rounded-md border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">{actionError}</div>}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <div className="rounded-lg border bg-white shadow-sm"><div className="flex items-center justify-between border-b px-5 py-4"><h2 className="font-semibold">Order items</h2><PurchaseStatus status={order.status} /></div><div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Product</th><th className="px-4 py-3 text-right">Quantity</th><th className="px-4 py-3 text-right">Unit Price</th><th className="px-4 py-3 text-right">Tax</th><th className="px-4 py-3 text-right">Line Total</th></tr></thead><tbody className="divide-y">{order.lines.map((line) => <tr key={line.id}><td className="px-4 py-3"><p className="font-medium">{line.product.name}</p><p className="font-mono text-xs text-muted-foreground">{line.product.sku}</p></td><td className="px-4 py-3 text-right tabular-nums">{line.quantity}</td><td className="px-4 py-3 text-right tabular-nums">{formatMoney(line.unitPrice)}</td><td className="px-4 py-3 text-right tabular-nums">{line.taxRate.toFixed(2)}%<p className="text-xs text-muted-foreground">{formatMoney(line.lineTax)}</p></td><td className="px-4 py-3 text-right font-medium tabular-nums">{formatMoney(line.lineTotal)}</td></tr>)}</tbody></table></div></div>
          {order.note && <div className="rounded-lg border bg-white p-5 shadow-sm"><h2 className="font-semibold">Note</h2><p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{order.note}</p></div>}
        </div>
        <div className="space-y-6">
          <div className="rounded-lg border bg-white p-5 shadow-sm"><h2 className="font-semibold">Totals</h2><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between"><dt className="text-muted-foreground">Subtotal</dt><dd>{formatMoney(order.subtotal)}</dd></div><div className="flex justify-between"><dt className="text-muted-foreground">Discount</dt><dd>{formatMoney(order.discountAmount)}</dd></div><div className="flex justify-between"><dt className="text-muted-foreground">Tax</dt><dd>{formatMoney(order.taxAmount)}</dd></div><div className="flex justify-between border-t pt-3 text-base font-semibold"><dt>Total</dt><dd>{formatMoney(order.totalAmount)}</dd></div></dl><p className="mt-3 text-xs text-muted-foreground">These totals were calculated by the backend.</p></div>
          <div className="rounded-lg border bg-white p-5 shadow-sm"><h2 className="font-semibold">Timeline</h2><dl className="mt-4 space-y-3 text-sm"><div><dt className="text-xs text-muted-foreground">Created</dt><dd>{timestampFormatter.format(new Date(order.createdAt))}</dd></div><div><dt className="text-xs text-muted-foreground">Last updated</dt><dd>{timestampFormatter.format(new Date(order.updatedAt))}</dd></div>{order.orderedAt && <div><dt className="text-xs text-muted-foreground">Ordered</dt><dd>{timestampFormatter.format(new Date(order.orderedAt))}</dd></div>}{order.receivedAt && <div><dt className="text-xs text-muted-foreground">Received</dt><dd>{timestampFormatter.format(new Date(order.receivedAt))}</dd></div>}<div><dt className="text-xs text-muted-foreground">Created by</dt><dd>{order.createdBy.firstName} {order.createdBy.lastName}</dd></div></dl></div>
        </div>
      </div>
      <ConfirmDialog confirmLabel={pendingAction ? actionCopy[pendingAction].confirmLabel : "Continue"} description={pendingAction ? actionCopy[pendingAction].description : ""} onConfirm={() => void runAction()} onOpenChange={(open) => { if (!open && !actionMutation.isPending) setPendingAction(null) }} open={pendingAction !== null} pending={actionMutation.isPending} title={pendingAction ? actionCopy[pendingAction].title : "Confirm action"} variant={pendingAction === "cancel" ? "destructive" : "default"} />
    </section>
  )
}
