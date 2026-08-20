import { useState } from "react"
import { Archive, FilterX, LoaderCircle, Pencil, Plus, Search } from "lucide-react"

import { ApiError } from "@/api/types"
import { ConfirmDialog } from "@/components/common/confirm-dialog"
import { EmptyState } from "@/components/common/empty-state"
import { ErrorState } from "@/components/common/error-state"
import { LoadingSkeleton } from "@/components/common/loading-skeleton"
import { PageHeader } from "@/components/common/page-header"
import { Pagination } from "@/components/common/pagination"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { CategoryFormDialog } from "@/features/catalog/components/category-form-dialog"
import { useArchiveCategory, useCategories } from "@/features/catalog/hooks/use-categories"
import type { Category } from "@/features/catalog/types"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { notifications } from "@/lib/notifications"

const PAGE_SIZE = 20
const selectClassName = "h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"

export function CategoriesPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [active, setActive] = useState("")
  const [formCategory, setFormCategory] = useState<Category | "create" | null>(null)
  const [archiveTarget, setArchiveTarget] = useState<Category | null>(null)
  const debouncedSearch = useDebouncedValue(search.trim())
  const query = useCategories({ page, size: PAGE_SIZE, search: debouncedSearch || undefined, active: active === "active" ? true : active === "archived" ? false : undefined })
  const archiveMutation = useArchiveCategory()
  const categories = query.data?.data ?? []
  const hasFilters = Boolean(search || active)

  async function archive() {
    if (!archiveTarget) return
    try {
      await archiveMutation.mutateAsync({ id: archiveTarget.id, version: archiveTarget.version })
      notifications.success("Category archived", `${archiveTarget.name} is no longer active.`)
      setArchiveTarget(null)
      if (categories.length === 1 && page > 0) setPage(page - 1)
    } catch (error) {
      notifications.error("Unable to archive category", error instanceof ApiError ? error.message : "Please try again.")
      if (error instanceof ApiError && error.code === "VERSION_CONFLICT") setArchiveTarget(null)
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader title="Categories" description="Organize products into clear catalog groups." actions={<Button onClick={() => setFormCategory("create")}><Plus className="size-4" />Add category</Button>} />
      <div className="rounded-lg border bg-white shadow-sm">
        <div className="grid gap-3 border-b p-4 sm:grid-cols-[minmax(240px,1fr)_180px_auto]">
          <div className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" maxLength={120} placeholder="Search categories…" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} /></div>
          <select aria-label="Filter by active status" className={selectClassName} value={active} onChange={(event) => { setActive(event.target.value); setPage(0) }}><option value="">All statuses</option><option value="active">Active</option><option value="archived">Archived</option></select>
          <Button disabled={!hasFilters} onClick={() => { setSearch(""); setActive(""); setPage(0) }} variant="ghost"><FilterX className="size-4" />Clear</Button>
        </div>
        {query.isFetching && !query.isLoading && <div className="flex items-center gap-2 border-b px-4 py-2 text-xs text-muted-foreground"><LoaderCircle className="size-3 animate-spin" />Updating categories…</div>}
        {query.isLoading ? <LoadingSkeleton rows={7} /> : query.isError ? <div className="p-4"><ErrorState message={query.error instanceof ApiError ? query.error.message : "Categories could not be loaded."} onRetry={() => void query.refetch()} /></div> : categories.length === 0 ? (
          <EmptyState title={hasFilters ? "No matching categories" : "No categories yet"} description={hasFilters ? "Try changing or clearing your filters." : "Add a category to organize the product catalog."} action={!hasFilters ? <Button onClick={() => setFormCategory("create")}>Add category</Button> : undefined} />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[680px] text-left text-sm">
                <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Description</th><th className="px-4 py-3">Status</th><th className="px-4 py-3 text-right">Actions</th></tr></thead>
                <tbody className="divide-y">{categories.map((category) => <tr className="hover:bg-slate-50/60" key={category.id}><td className="px-4 py-3 font-medium">{category.name}</td><td className="max-w-md px-4 py-3 text-muted-foreground"><span className="line-clamp-2">{category.description || "—"}</span></td><td className="px-4 py-3"><StatusBadge variant={category.active ? "success" : "neutral"}>{category.active ? "Active" : "Archived"}</StatusBadge></td><td className="px-4 py-3"><div className="flex justify-end gap-1"><Button onClick={() => setFormCategory(category)} size="icon" title={`Edit ${category.name}`} variant="ghost"><Pencil className="size-4" /><span className="sr-only">Edit</span></Button><Button disabled={!category.active} onClick={() => setArchiveTarget(category)} size="icon" title={`Archive ${category.name}`} variant="ghost"><Archive className="size-4" /><span className="sr-only">Archive</span></Button></div></td></tr>)}</tbody>
              </table>
            </div>
            {query.data?.page && <Pagination page={query.data.page.number} pageSize={query.data.page.size} totalElements={query.data.page.totalElements} totalPages={query.data.page.totalPages} onPageChange={setPage} />}
          </>
        )}
      </div>
      {formCategory && <CategoryFormDialog category={formCategory === "create" ? null : formCategory} key={formCategory === "create" ? "create" : formCategory.id} onClose={() => setFormCategory(null)} />}
      <ConfirmDialog confirmLabel="Archive category" description={archiveTarget ? `Archive ${archiveTarget.name}? Products already assigned to it will not be changed.` : ""} onConfirm={() => void archive()} onOpenChange={(open) => { if (!open && !archiveMutation.isPending) setArchiveTarget(null) }} open={archiveTarget !== null} pending={archiveMutation.isPending} title="Archive category" />
    </section>
  )
}
