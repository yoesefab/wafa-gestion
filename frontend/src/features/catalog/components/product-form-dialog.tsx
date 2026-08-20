import { useState, type FormEvent } from "react"
import { LoaderCircle } from "lucide-react"

import { ApiError } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useCreateProduct, useUpdateProduct } from "@/features/catalog/hooks/use-products"
import type { Category, Product, ProductWriteRequest, UnitOfMeasure } from "@/features/catalog/types"
import { notifications } from "@/lib/notifications"

const selectClassName = "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
const units: { value: UnitOfMeasure; label: string }[] = [
  { value: "PIECE", label: "Piece" },
  { value: "BOX", label: "Box" },
  { value: "PACK", label: "Pack" },
]

interface FormValues {
  sku: string
  name: string
  categoryId: string
  unitOfMeasure: UnitOfMeasure
  purchasePrice: string
  sellingPrice: string
  minimumStock: string
}

function initialValues(product: Product | null): FormValues {
  return product ? {
    sku: product.sku,
    name: product.name,
    categoryId: String(product.category.id),
    unitOfMeasure: product.unitOfMeasure,
    purchasePrice: String(product.purchasePrice),
    sellingPrice: String(product.sellingPrice),
    minimumStock: String(product.minimumStock),
  } : { sku: "", name: "", categoryId: "", unitOfMeasure: "PIECE", purchasePrice: "", sellingPrice: "", minimumStock: "0" }
}

export function ProductFormDialog({ product, categories, onClose }: { product: Product | null; categories: Category[]; onClose: () => void }) {
  const [values, setValues] = useState(() => initialValues(product))
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState("")
  const createMutation = useCreateProduct()
  const updateMutation = useUpdateProduct()
  const mutation = product ? updateMutation : createMutation
  const activeCategories = categories.filter((category) => category.active)

  function updateField<K extends keyof FormValues>(field: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: "" }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError("")
    setFieldErrors({})

    const request: ProductWriteRequest = {
      sku: values.sku.trim(),
      name: values.name.trim(),
      categoryId: Number(values.categoryId),
      unitOfMeasure: values.unitOfMeasure,
      purchasePrice: Number(values.purchasePrice),
      sellingPrice: Number(values.sellingPrice),
      minimumStock: Number(values.minimumStock),
    }

    try {
      if (product) await updateMutation.mutateAsync({ id: product.id, request: { ...request, version: product.version } })
      else await createMutation.mutateAsync(request)
      notifications.success(product ? "Product updated" : "Product created", `${request.name} was saved successfully.`)
      onClose()
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(Object.fromEntries(error.fieldErrors.map((fieldError) => [fieldError.field, fieldError.message])))
        setFormError(error.code === "VERSION_CONFLICT" ? "This product changed since you opened it. Close the form and try again." : error.message)
      } else setFormError("The product could not be saved. Please try again.")
    }
  }

  const field = (name: keyof FormValues, label: string, input: React.ReactNode) => (
    <div className="space-y-2">
      <label className="text-sm font-medium" htmlFor={`product-${name}`}>{label}</label>
      {input}
      {fieldErrors[name] && <p className="text-xs text-destructive">{fieldErrors[name]}</p>}
    </div>
  )

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose() }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold">{product ? "Edit product" : "Add product"}</DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">Product stock is managed separately through inventory movements.</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            {field("sku", "Reference", <Input id="product-sku" maxLength={80} required value={values.sku} onChange={(event) => updateField("sku", event.target.value)} />)}
            {field("name", "Name", <Input id="product-name" maxLength={180} required value={values.name} onChange={(event) => updateField("name", event.target.value)} />)}
            <div className="space-y-2 sm:col-span-2">
              <label className="text-sm font-medium" htmlFor="product-categoryId">Category</label>
              <select className={selectClassName} id="product-categoryId" required value={values.categoryId} onChange={(event) => updateField("categoryId", event.target.value)}>
                <option value="">Select a category</option>
                {activeCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
              {fieldErrors.categoryId && <p className="text-xs text-destructive">{fieldErrors.categoryId}</p>}
              {activeCategories.length === 0 && <p className="text-xs text-amber-700">Create an active category before adding a product.</p>}
            </div>
            {field("unitOfMeasure", "Unit of measure", <select className={selectClassName} id="product-unitOfMeasure" required value={values.unitOfMeasure} onChange={(event) => updateField("unitOfMeasure", event.target.value as UnitOfMeasure)}>{units.map((unit) => <option key={unit.value} value={unit.value}>{unit.label}</option>)}</select>)}
            {field("minimumStock", "Minimum stock", <Input id="product-minimumStock" min="0" required step="1" type="number" value={values.minimumStock} onChange={(event) => updateField("minimumStock", event.target.value)} />)}
            {field("purchasePrice", "Purchase price (MAD)", <Input id="product-purchasePrice" min="0" required step="0.01" type="number" value={values.purchasePrice} onChange={(event) => updateField("purchasePrice", event.target.value)} />)}
            {field("sellingPrice", "Selling price (MAD)", <Input id="product-sellingPrice" min="0" required step="0.01" type="number" value={values.sellingPrice} onChange={(event) => updateField("sellingPrice", event.target.value)} />)}
          </div>
          {product && <div className="mt-4 rounded-md bg-secondary px-3 py-2 text-sm text-muted-foreground">Current stock: <span className="font-medium text-foreground">{product.currentStock}</span>. Use Inventory to adjust it.</div>}
          {formError && <p className="mt-4 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">{formError}</p>}
          <DialogFooter>
            <DialogClose asChild><Button disabled={mutation.isPending} type="button" variant="outline">Cancel</Button></DialogClose>
            <Button disabled={mutation.isPending || activeCategories.length === 0} type="submit">
              {mutation.isPending && <LoaderCircle className="size-4 animate-spin" />}{mutation.isPending ? "Saving…" : "Save product"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
