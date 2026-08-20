import { FormEvent, useMemo, useState } from "react"
import { ConfirmDialog } from "@/components/common/confirm-dialog"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useCreateStockAdjustment } from "@/features/inventory/hooks/use-inventory"
import { inventoryErrorMessage } from "@/features/inventory/lib/errors"
import type { AdjustmentDirection } from "@/features/inventory/types"
import type { Product } from "@/features/catalog/types"
import { notifications } from "@/lib/notifications"

const selectClassName = "h-10 w-full rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
const labelClassName = "space-y-1.5 text-sm font-medium"

interface StockAdjustmentDialogProps {
  products: Product[]
  onClose: () => void
}

export function StockAdjustmentDialog({ products, onClose }: StockAdjustmentDialogProps) {
  const [productId, setProductId] = useState("")
  const [direction, setDirection] = useState<AdjustmentDirection>("IN")
  const [quantity, setQuantity] = useState("")
  const [reference, setReference] = useState("")
  const [reason, setReason] = useState("")
  const [note, setNote] = useState("")
  const [confirming, setConfirming] = useState(false)
  const mutation = useCreateStockAdjustment()
  const activeProducts = useMemo(() => products.filter((product) => product.active), [products])
  const selectedProduct = activeProducts.find((product) => product.id === Number(productId))
  const parsedQuantity = Number(quantity)
  const valid = Boolean(selectedProduct && Number.isInteger(parsedQuantity) && parsedQuantity > 0 && reference.trim() && reason.trim())

  function review(event: FormEvent) {
    event.preventDefault()
    if (valid) setConfirming(true)
  }

  async function submit() {
    if (!selectedProduct || !valid) return
    try {
      await mutation.mutateAsync({
        productId: selectedProduct.id,
        request: {
          direction,
          quantity: parsedQuantity,
          reference: reference.trim(),
          reason: reason.trim(),
          note: note.trim() || undefined,
        },
      })
      notifications.success("Stock adjusted", `${selectedProduct.name}'s stock and movement history have been refreshed.`)
      setConfirming(false)
      onClose()
    } catch (error) {
      notifications.error("Unable to adjust stock", inventoryErrorMessage(error))
      setConfirming(false)
    }
  }

  return (
    <>
      <Dialog open onOpenChange={(open) => { if (!open && !mutation.isPending && !confirming) onClose() }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="text-lg font-semibold">Manual stock adjustment</DialogTitle>
            <DialogDescription className="text-sm text-muted-foreground">Record a justified stock correction. The backend validates and calculates the resulting stock.</DialogDescription>
          </DialogHeader>
          <form className="mt-5 space-y-4" onSubmit={review}>
            <label className={labelClassName}>Product
              <select className={selectClassName} required value={productId} onChange={(event) => setProductId(event.target.value)}>
                <option value="">Choose a product</option>
                {activeProducts.map((product) => <option key={product.id} value={product.id}>{product.sku} — {product.name}</option>)}
              </select>
            </label>
            <div className="rounded-md border bg-slate-50 px-4 py-3">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Current stock</p>
              <p className="mt-1 text-xl font-semibold tabular-nums">{selectedProduct ? selectedProduct.currentStock : "—"}</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className={labelClassName}>Direction
                <select className={selectClassName} value={direction} onChange={(event) => setDirection(event.target.value as AdjustmentDirection)}><option value="IN">Add stock</option><option value="OUT">Remove stock</option></select>
              </label>
              <label className={labelClassName}>Quantity
                <Input min={1} required step={1} type="number" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
              </label>
            </div>
            <label className={labelClassName}>Reference
              <Input maxLength={120} placeholder="e.g. COUNT-2026-08" required value={reference} onChange={(event) => setReference(event.target.value)} />
            </label>
            <label className={labelClassName}>Reason
              <Input maxLength={180} placeholder="Why is this adjustment needed?" required value={reason} onChange={(event) => setReason(event.target.value)} />
            </label>
            <label className={labelClassName}>Note <span className="font-normal text-muted-foreground">(optional)</span>
              <textarea className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm" maxLength={1000} value={note} onChange={(event) => setNote(event.target.value)} />
            </label>
            {mutation.isError && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{inventoryErrorMessage(mutation.error)}</p>}
            <DialogFooter>
              <Button disabled={mutation.isPending} onClick={onClose} type="button" variant="outline">Cancel</Button>
              <Button disabled={!valid || mutation.isPending} type="submit">Review adjustment</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        confirmLabel="Confirm adjustment"
        description={selectedProduct ? `${direction === "IN" ? "Add" : "Remove"} ${parsedQuantity || 0} unit${parsedQuantity === 1 ? "" : "s"} ${direction === "IN" ? "to" : "from"} ${selectedProduct.name}? This creates a permanent inventory movement; final stock is calculated by the backend.` : ""}
        onConfirm={() => void submit()}
        onOpenChange={(open) => { if (!open && !mutation.isPending) setConfirming(false) }}
        open={confirming}
        pending={mutation.isPending}
        title="Confirm stock adjustment"
        variant="default"
      />
    </>
  )
}
