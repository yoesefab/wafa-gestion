import { Archive, Pencil } from "lucide-react"

import { EmptyState } from "@/components/common/empty-state"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import type { Product } from "@/features/catalog/types"

const moneyFormatter = new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD", minimumFractionDigits: 2 })

function StockValue({ product }: { product: Product }) {
  return (
    <div className="flex items-center gap-2">
      <span className={product.lowStock ? "font-semibold text-amber-700" : ""}>{product.currentStock}</span>
      {product.lowStock && <StatusBadge variant="warning">Low</StatusBadge>}
    </div>
  )
}

function Actions({ product, onEdit, onArchive }: { product: Product; onEdit: (product: Product) => void; onArchive: (product: Product) => void }) {
  return (
    <div className="flex justify-end gap-1">
      <Button onClick={() => onEdit(product)} size="icon" title={`Edit ${product.name}`} variant="ghost"><Pencil className="size-4" /><span className="sr-only">Edit</span></Button>
      <Button disabled={!product.active} onClick={() => onArchive(product)} size="icon" title={`Archive ${product.name}`} variant="ghost"><Archive className="size-4" /><span className="sr-only">Archive</span></Button>
    </div>
  )
}

export function ProductsTable({ products, hasFilters, onAdd, onEdit, onArchive }: { products: Product[]; hasFilters: boolean; onAdd: () => void; onEdit: (product: Product) => void; onArchive: (product: Product) => void }) {
  if (products.length === 0) {
    return <EmptyState title={hasFilters ? "No matching products" : "No products yet"} description={hasFilters ? "Try changing or clearing your filters." : "Add your first product to start building the catalog."} action={!hasFilters ? <Button onClick={onAdd}>Add product</Button> : undefined} />
  }

  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[1120px] text-left text-sm">
          <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground">
            <tr><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Name</th><th className="px-4 py-3">Category</th><th className="px-4 py-3 text-right">Purchase Price</th><th className="px-4 py-3 text-right">Selling Price</th><th className="px-4 py-3">Stock</th><th className="px-4 py-3">Minimum Stock</th><th className="px-4 py-3">Status</th><th className="px-4 py-3 text-right">Actions</th></tr>
          </thead>
          <tbody className="divide-y">
            {products.map((product) => (
              <tr className="hover:bg-slate-50/60" key={product.id}>
                <td className="px-4 py-3 font-mono text-xs font-medium">{product.sku}</td>
                <td className="px-4 py-3 font-medium">{product.name}</td>
                <td className="px-4 py-3 text-muted-foreground">{product.category.name}</td>
                <td className="px-4 py-3 text-right tabular-nums">{moneyFormatter.format(product.purchasePrice)}</td>
                <td className="px-4 py-3 text-right tabular-nums">{moneyFormatter.format(product.sellingPrice)}</td>
                <td className="px-4 py-3"><StockValue product={product} /></td>
                <td className="px-4 py-3 tabular-nums">{product.minimumStock}</td>
                <td className="px-4 py-3"><StatusBadge variant={product.active ? "success" : "neutral"}>{product.active ? "Active" : "Archived"}</StatusBadge></td>
                <td className="px-4 py-3"><Actions product={product} onArchive={onArchive} onEdit={onEdit} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="divide-y md:hidden">
        {products.map((product) => (
          <article className="p-4" key={product.id}>
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0"><p className="truncate font-medium">{product.name}</p><p className="mt-1 font-mono text-xs text-muted-foreground">{product.sku}</p></div>
              <StatusBadge variant={product.active ? "success" : "neutral"}>{product.active ? "Active" : "Archived"}</StatusBadge>
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
              <div><dt className="text-xs text-muted-foreground">Category</dt><dd className="mt-0.5 truncate">{product.category.name}</dd></div>
              <div><dt className="text-xs text-muted-foreground">Stock / Minimum</dt><dd className="mt-0.5 flex items-center gap-2"><StockValue product={product} /><span className="text-muted-foreground">/ {product.minimumStock}</span></dd></div>
              <div><dt className="text-xs text-muted-foreground">Purchase</dt><dd className="mt-0.5">{moneyFormatter.format(product.purchasePrice)}</dd></div>
              <div><dt className="text-xs text-muted-foreground">Selling</dt><dd className="mt-0.5">{moneyFormatter.format(product.sellingPrice)}</dd></div>
            </dl>
            <div className="mt-3 border-t pt-2"><Actions product={product} onArchive={onArchive} onEdit={onEdit} /></div>
          </article>
        ))}
      </div>
    </>
  )
}
