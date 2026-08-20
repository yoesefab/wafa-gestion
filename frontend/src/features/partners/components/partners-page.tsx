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
import { PartnerFormDialog } from "@/features/partners/components/partner-form-dialog"
import { PartnersTable } from "@/features/partners/components/partners-table"
import { useArchivePartner, usePartners } from "@/features/partners/hooks/use-partners"
import type { Partner, PartnerKind, PartnerResource } from "@/features/partners/types"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { notifications } from "@/lib/notifications"

const PAGE_SIZE = 20
const selectClassName = "h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"

export function PartnersPage({ kind }: { kind: PartnerKind }) {
  const resource: PartnerResource = kind === "customer" ? "customers" : "suppliers"
  const title = kind === "customer" ? "Customers" : "Suppliers"
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [active, setActive] = useState("")
  const [formPartner, setFormPartner] = useState<Partner | "create" | null>(null)
  const [archiveTarget, setArchiveTarget] = useState<Partner | null>(null)
  const debouncedSearch = useDebouncedValue(search.trim())
  const query = usePartners(resource, { page, size: PAGE_SIZE, search: debouncedSearch || undefined, active: active === "active" ? true : active === "archived" ? false : undefined })
  const archiveMutation = useArchivePartner(resource)
  const partners = query.data?.data ?? []
  const hasFilters = Boolean(search || active)

  async function archive() {
    if (!archiveTarget) return
    try {
      await archiveMutation.mutateAsync({ id: archiveTarget.id, version: archiveTarget.version })
      notifications.success(`${kind === "customer" ? "Customer" : "Supplier"} archived`, `${archiveTarget.name} is no longer active.`)
      setArchiveTarget(null)
      if (partners.length === 1 && page > 0) setPage(page - 1)
    } catch (error) {
      notifications.error(`Unable to archive ${kind}`, error instanceof ApiError ? error.message : "Please try again.")
      if (error instanceof ApiError && error.code === "VERSION_CONFLICT") setArchiveTarget(null)
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader title={title} description={`Manage ${kind} company and contact information.`} actions={<Button onClick={() => setFormPartner("create")}><Plus className="size-4" />Add {kind}</Button>} />
      <div className="rounded-lg border bg-white shadow-sm">
        <div className="grid gap-3 border-b p-4 sm:grid-cols-[minmax(240px,1fr)_180px_auto]">
          <div className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" maxLength={254} placeholder="Search name, ICE, email, or phone…" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} /></div>
          <select aria-label="Filter by active status" className={selectClassName} value={active} onChange={(event) => { setActive(event.target.value); setPage(0) }}><option value="">All statuses</option><option value="active">Active</option><option value="archived">Archived</option></select>
          <Button disabled={!hasFilters} onClick={() => { setSearch(""); setActive(""); setPage(0) }} variant="ghost"><FilterX className="size-4" />Clear</Button>
        </div>
        {query.isFetching && !query.isLoading && <div className="flex items-center gap-2 border-b px-4 py-2 text-xs text-muted-foreground"><LoaderCircle className="size-3 animate-spin" />Updating {resource}…</div>}
        {query.isLoading ? <LoadingSkeleton rows={8} /> : query.isError ? <div className="p-4"><ErrorState message={query.error instanceof ApiError ? query.error.message : `${title} could not be loaded.`} onRetry={() => void query.refetch()} /></div> : (
          <><PartnersTable hasFilters={hasFilters} kind={kind} onAdd={() => setFormPartner("create")} onArchive={setArchiveTarget} onEdit={setFormPartner} partners={partners} />{query.data?.page && <Pagination page={query.data.page.number} pageSize={query.data.page.size} totalElements={query.data.page.totalElements} totalPages={query.data.page.totalPages} onPageChange={setPage} />}</>
        )}
      </div>
      {formPartner && <PartnerFormDialog kind={kind} key={formPartner === "create" ? "create" : formPartner.id} onClose={() => setFormPartner(null)} partner={formPartner === "create" ? null : formPartner} resource={resource} />}
      <ConfirmDialog confirmLabel={`Archive ${kind}`} description={archiveTarget ? `Archive ${archiveTarget.name}? Existing transactional records will remain unchanged.` : ""} onConfirm={() => void archive()} onOpenChange={(open) => { if (!open && !archiveMutation.isPending) setArchiveTarget(null) }} open={archiveTarget !== null} pending={archiveMutation.isPending} title={`Archive ${kind}`} />
    </section>
  )
}
