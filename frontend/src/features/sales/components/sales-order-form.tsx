import { useState, type FormEvent } from "react"
import { LoaderCircle, Plus, Trash2 } from "lucide-react"
import { Link, useNavigate } from "react-router-dom"

import { ApiError } from "@/api/types"
import { ErrorState } from "@/components/common/error-state"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useCreateSalesOrder, useSalesSelectors, useUpdateSalesOrder } from "@/features/sales/hooks/use-sales-orders"
import type { SalesOrderDetail, SalesOrderWriteRequest } from "@/features/sales/types"
import { calculateLine, decimalToScaled, formatCents } from "@/lib/money"
import { notifications } from "@/lib/notifications"

const selectClassName = "h-10 w-full rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50"

interface DraftLine {
  key: number
  productId: string
  quantity: string
  taxRate: string
  fallbackName?: string
  fallbackPrice?: number
}

let nextLineKey = 1
const today = () => {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function initialLines(order?: SalesOrderDetail): DraftLine[] {
  if (!order) return [{ key: nextLineKey++, productId: "", quantity: "1", taxRate: "20" }]
  return order.lines.map((line) => ({ key: nextLineKey++, productId: String(line.product.id), quantity: String(line.quantity), taxRate: String(line.taxRate), fallbackName: `${line.product.sku} — ${line.product.name}`, fallbackPrice: line.unitPrice }))
}

function businessMessage(error: ApiError) {
  if (error.code === "INACTIVE_REFERENCE") return "The selected customer or product is archived. Choose an active record and try again."
  if (error.code === "DUPLICATE_ORDER_ITEM") return "Each product can appear only once. Update the existing line quantity instead."
  if (error.code === "VERSION_CONFLICT") return "This draft changed since it was loaded. Reload the order and try again."
  if (error.code === "INVALID_STATE_TRANSITION") return "This order is no longer editable because its status changed."
  return error.message
}

export function SalesOrderForm({ order, onSaved }: { order?: SalesOrderDetail; onSaved?: () => void }) {
  const navigate = useNavigate()
  const [customerId, setCustomerId] = useState(order ? String(order.customer.id) : "")
  const [orderDate, setOrderDate] = useState(order?.orderDate ?? today())
  const [note, setNote] = useState(order?.note ?? "")
  const [lines, setLines] = useState(() => initialLines(order))
  const [formError, setFormError] = useState("")
  const { customers, products } = useSalesSelectors()
  const createMutation = useCreateSalesOrder()
  const updateMutation = useUpdateSalesOrder()
  const mutation = order ? updateMutation : createMutation

  if (customers.isLoading || products.isLoading) return <div className="rounded-lg border bg-white p-6"><div className="animate-pulse space-y-4">{Array.from({ length: 6 }, (_, index) => <div className="h-12 rounded bg-slate-100" key={index} />)}</div></div>
  if (customers.isError || products.isError) return <ErrorState message="Customers or products could not be loaded for this order." onRetry={() => { void customers.refetch(); void products.refetch() }} />

  const availableCustomers = customers.data ?? []
  const availableProducts = products.data ?? []
  const selectedIds = new Set(lines.map((line) => line.productId).filter(Boolean))
  const previews = lines.map((line) => {
    const product = availableProducts.find((item) => item.id === Number(line.productId))
    const price = product?.sellingPrice ?? line.fallbackPrice ?? 0
    return { product, price, ...calculateLine(line.quantity, price, line.taxRate) }
  })
  const subtotal = previews.reduce((sum, line) => sum + line.subtotal, 0n)
  const tax = previews.reduce((sum, line) => sum + line.tax, 0n)
  const total = subtotal + tax
  const hasUnavailableLine = lines.some((line, index) => line.productId && !previews[index].product)

  function updateLine(key: number, patch: Partial<DraftLine>) {
    setLines((current) => current.map((line) => line.key === key ? { ...line, ...patch } : line))
    setFormError("")
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError("")
    if (hasUnavailableLine) { setFormError("Remove or replace archived products before saving this draft."); return }
    if (lines.length === 0 || lines.some((line) => !line.productId || Number(line.quantity) <= 0 || !Number.isInteger(Number(line.quantity)))) { setFormError("Add at least one product with a positive whole quantity."); return }

    const request: SalesOrderWriteRequest = {
      customerId: Number(customerId),
      orderDate,
      note: note.trim() || null,
      lines: lines.map((line) => ({ productId: Number(line.productId), quantity: Number(line.quantity), taxRate: Number(line.taxRate) })),
    }
    try {
      const saved = order ? await updateMutation.mutateAsync({ id: order.id, request: { ...request, version: order.version } }) : await createMutation.mutateAsync(request)
      notifications.success(order ? "Draft updated" : "Draft created", `${saved.orderNumber} was saved successfully.`)
      if (onSaved) onSaved()
      else navigate(`/sales/${saved.id}`, { replace: true })
    } catch (error) {
      setFormError(error instanceof ApiError ? businessMessage(error) : "The draft could not be saved. Please try again.")
    }
  }

  return (
    <form className="space-y-6" onSubmit={submit}>
      <div className="rounded-lg border bg-white p-5 shadow-sm">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2"><label className="text-sm font-medium" htmlFor="sales-customer">Customer</label><select className={selectClassName} id="sales-customer" required value={customerId} onChange={(event) => setCustomerId(event.target.value)}><option value="">Select a customer</option>{availableCustomers.map((customer) => <option key={customer.id} value={customer.id}>{customer.name}</option>)}</select>{availableCustomers.length === 0 && <p className="text-xs text-amber-700">An active customer is required.</p>}</div>
          <div className="space-y-2"><label className="text-sm font-medium" htmlFor="sales-date">Order date</label><Input id="sales-date" required type="date" value={orderDate} onChange={(event) => setOrderDate(event.target.value)} /></div>
          <div className="space-y-2 sm:col-span-2"><label className="text-sm font-medium" htmlFor="sales-note">Note</label><textarea className="min-h-20 w-full resize-y rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2" id="sales-note" maxLength={1000} value={note} onChange={(event) => setNote(event.target.value)} /></div>
        </div>
      </div>

      <div className="rounded-lg border bg-white shadow-sm">
        <div className="flex items-center justify-between border-b px-5 py-4"><div><h2 className="font-semibold">Products</h2><p className="text-xs text-muted-foreground">Prices and totals shown here are previews.</p></div><Button disabled={availableProducts.length === 0} onClick={() => setLines((current) => [...current, { key: nextLineKey++, productId: "", quantity: "1", taxRate: "20" }])} size="sm" type="button" variant="outline"><Plus className="size-4" />Add line</Button></div>
        <div className="divide-y">
          {lines.map((line, index) => {
            const preview = previews[index]
            return <div className="grid gap-3 p-4 lg:grid-cols-[minmax(240px,1fr)_110px_140px_140px_44px] lg:items-end" key={line.key}>
              <div className="space-y-2"><label className="text-xs font-medium text-muted-foreground" htmlFor={`line-product-${line.key}`}>Product</label><select className={selectClassName} id={`line-product-${line.key}`} required value={line.productId} onChange={(event) => updateLine(line.key, { productId: event.target.value, fallbackName: undefined, fallbackPrice: undefined })}><option value="">Select a product</option>{line.fallbackName && !preview.product && <option value={line.productId}>{line.fallbackName} (Unavailable)</option>}{availableProducts.map((product) => <option disabled={selectedIds.has(String(product.id)) && String(product.id) !== line.productId} key={product.id} value={product.id}>{product.sku} — {product.name} (stock {product.currentStock})</option>)}</select></div>
              <div className="space-y-2"><label className="text-xs font-medium text-muted-foreground" htmlFor={`line-quantity-${line.key}`}>Quantity</label><Input id={`line-quantity-${line.key}`} min="1" required step="1" type="number" value={line.quantity} onChange={(event) => updateLine(line.key, { quantity: event.target.value })} /></div>
              <div className="space-y-2"><label className="text-xs font-medium text-muted-foreground" htmlFor={`line-tax-${line.key}`}>Tax rate (%)</label><Input id={`line-tax-${line.key}`} max="100" min="0" required step="0.01" type="number" value={line.taxRate} onChange={(event) => updateLine(line.key, { taxRate: event.target.value })} /></div>
              <div><p className="text-xs font-medium text-muted-foreground">Current selling price / Line total</p><p className="mt-2 text-sm">{formatCents(decimalToScaled(preview.price, 2))}</p><p className="text-xs text-muted-foreground">{formatCents(preview.total)}</p></div>
              <Button disabled={lines.length === 1} onClick={() => setLines((current) => current.filter((item) => item.key !== line.key))} size="icon" title="Remove line" type="button" variant="ghost"><Trash2 className="size-4 text-destructive" /><span className="sr-only">Remove line</span></Button>
            </div>
          })}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div>{hasUnavailableLine && <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">This draft contains an archived or unavailable product. Replace or remove it before saving.</p>}{formError && <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">{formError}</p>}</div>
        <div className="rounded-lg border bg-white p-5 shadow-sm"><h2 className="font-semibold">Totals preview</h2><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between"><dt className="text-muted-foreground">Subtotal</dt><dd>{formatCents(subtotal)}</dd></div><div className="flex justify-between"><dt className="text-muted-foreground">Discount (not supported)</dt><dd>{formatCents(0n)}</dd></div><div className="flex justify-between"><dt className="text-muted-foreground">Tax</dt><dd>{formatCents(tax)}</dd></div><div className="flex justify-between border-t pt-3 text-base font-semibold"><dt>Total</dt><dd>{formatCents(total)}</dd></div></dl><p className="mt-3 text-xs text-muted-foreground">The backend recalculates all saved monetary values.</p></div>
      </div>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end"><Button asChild type="button" variant="outline"><Link to={order ? `/sales/${order.id}` : "/sales"}>Cancel</Link></Button><Button disabled={mutation.isPending || availableCustomers.length === 0 || availableProducts.length === 0 || hasUnavailableLine} type="submit">{mutation.isPending && <LoaderCircle className="size-4 animate-spin" />}{mutation.isPending ? "Saving draft…" : "Save draft"}</Button></div>
    </form>
  )
}
