import { useState } from "react"
import { FilterX, LoaderCircle, Plus, Search } from "lucide-react"

import { ApiError } from "@/api/types"
import { ConfirmDialog } from "@/components/common/confirm-dialog"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ProductFormDialog } from "@/features/catalog/components/product-form-dialog"
import { ProductsTable } from "@/features/catalog/components/products-table"
import { useArchiveProduct, useProductCategories, useProducts } from "@/features/catalog/hooks/use-products"
import type { Product } from "@/features/catalog/types"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { notifications } from "@/lib/notifications"

const PAGE_SIZE = 20
const selectClassName = "h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"

export function ProductsPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [categoryId, setCategoryId] = useState("")
  const [active, setActive] = useState("")
  const [lowStock, setLowStock] = useState(false)
  const [formProduct, setFormProduct] = useState<Product | "create" | null>(null)
  const [archiveTarget, setArchiveTarget] = useState<Product | null>(null)
  const debouncedSearch = useDebouncedValue(search.trim())

  const filters = {
    page,
    size: PAGE_SIZE,
    search: debouncedSearch || undefined,
    categoryId: categoryId ? Number(categoryId) : undefined,
    active: active === "active" ? true : active === "archived" ? false : undefined,
    lowStock: lowStock || undefined,
  }
  const productsQuery = useProducts(filters)
  const categoriesQuery = useProductCategories()
  const archiveMutation = useArchiveProduct()
  const hasFilters = Boolean(search || categoryId || active || lowStock)

  function resetPageAnd(action: () => void) {
    setPage(0)
    action()
  }

  function clearFilters() {
    setSearch("")
    setCategoryId("")
    setActive("")
    setLowStock(false)
    setPage(0)
  }

  async function confirmArchive() {
    if (!archiveTarget) return
    try {
      await archiveMutation.mutateAsync({ id: archiveTarget.id, version: archiveTarget.version })
      notifications.success("Product archived", `${archiveTarget.name} is no longer active.`)
      setArchiveTarget(null)
      if (products.length === 1 && page > 0) setPage(page - 1)
    } catch (error) {
      notifications.error("Unable to archive product", error instanceof ApiError ? error.message : "Please try again.")
      if (error instanceof ApiError && error.code === "VERSION_CONFLICT") setArchiveTarget(null)
    }
  }

  const products = productsQuery.data?.data ?? []
  const metadata = productsQuery.data?.page

  return (
    <section className="space-y-6">
      <PageHeader
        title="Products"
        description="Manage product details, prices, categories, and stock thresholds."
        actions={<Button disabled={categoriesQuery.isLoading || categoriesQuery.isError} onClick={() => setFormProduct("create")}><Plus className="size-4" />Add product</Button>}
      />

      <div className="rounded-lg border bg-white shadow-sm">
        <div className="grid gap-3 border-b p-4 lg:grid-cols-[minmax(240px,1fr)_220px_160px_150px_auto]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input className="pl-9" maxLength={180} placeholder="Search reference or name…" value={search} onChange={(event) => resetPageAnd(() => setSearch(event.target.value))} />
          </div>
          <select aria-label="Filter by category" className={selectClassName} disabled={categoriesQuery.isLoading || categoriesQuery.isError} value={categoryId} onChange={(event) => resetPageAnd(() => setCategoryId(event.target.value))}>
            <option value="">All categories</option>
            {categoriesQuery.data?.map((category) => <option key={category.id} value={category.id}>{category.name}{category.active ? "" : " (Archived)"}</option>)}
          </select>
          <select aria-label="Filter by active status" className={selectClassName} value={active} onChange={(event) => resetPageAnd(() => setActive(event.target.value))}>
            <option value="">All statuses</option><option value="active">Active</option><option value="archived">Archived</option>
          </select>
          <label className="flex h-10 cursor-pointer items-center gap-2 rounded-md border px-3 text-sm"><input checked={lowStock} className="size-4 accent-teal-600" onChange={(event) => resetPageAnd(() => setLowStock(event.target.checked))} type="checkbox" />Low stock only</label>
          <Button disabled={!hasFilters} onClick={clearFilters} variant="ghost"><FilterX className="size-4" />Clear</Button>
        </div>

        {categoriesQuery.isError && <p className="border-b bg-amber-50 px-4 py-2 text-sm text-amber-800">Categories could not be loaded. Category filtering and product forms are temporarily unavailable.</p>}
        {productsQuery.isFetching && !productsQuery.isLoading && <div className="flex items-center gap-2 border-b px-4 py-2 text-xs text-muted-foreground"><LoaderCircle className="size-3 animate-spin" />Updating products…</div>}

        {productsQuery.isLoading ? <LoadingSkeleton rows={8} /> : productsQuery.isError ? (
          <div className="p-4"><ErrorState message={productsQuery.error instanceof ApiError ? productsQuery.error.message : "Products could not be loaded."} onRetry={() => void productsQuery.refetch()} /></div>
        ) : (
          <>
            <ProductsTable products={products} hasFilters={hasFilters} onAdd={() => setFormProduct("create")} onArchive={setArchiveTarget} onEdit={setFormProduct} />
            {metadata && <Pagination page={metadata.number} pageSize={metadata.size} totalElements={metadata.totalElements} totalPages={metadata.totalPages} onPageChange={setPage} />}
          </>
        )}
      </div>

      {formProduct && categoriesQuery.data && <ProductFormDialog categories={categoriesQuery.data} key={formProduct === "create" ? "create" : formProduct.id} onClose={() => setFormProduct(null)} product={formProduct === "create" ? null : formProduct} />}
      <ConfirmDialog
        confirmLabel="Archive product"
        description={archiveTarget ? `Archive ${archiveTarget.name}? Existing orders and stock movements will remain unchanged.` : ""}
        onConfirm={() => void confirmArchive()}
        onOpenChange={(open) => { if (!open && !archiveMutation.isPending) setArchiveTarget(null) }}
        open={archiveTarget !== null}
        pending={archiveMutation.isPending}
        title="Archive product"
      />
    </section>
  )
}
