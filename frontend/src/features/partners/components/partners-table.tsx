import { Archive, Mail, Pencil, Phone } from "lucide-react"

import { EmptyState } from "@/components/common/empty-state"
import { StatusBadge } from "@/components/common/status-badge"
import { Button } from "@/components/ui/button"
import type { Partner, PartnerKind } from "@/features/partners/types"

function Actions({ partner, onEdit, onArchive }: { partner: Partner; onEdit: (partner: Partner) => void; onArchive: (partner: Partner) => void }) {
  return <div className="flex justify-end gap-1"><Button onClick={() => onEdit(partner)} size="icon" title={`Edit ${partner.name}`} variant="ghost"><Pencil className="size-4" /><span className="sr-only">Edit</span></Button><Button disabled={!partner.active} onClick={() => onArchive(partner)} size="icon" title={`Archive ${partner.name}`} variant="ghost"><Archive className="size-4" /><span className="sr-only">Archive</span></Button></div>
}

export function PartnersTable({ kind, partners, hasFilters, onAdd, onEdit, onArchive }: { kind: PartnerKind; partners: Partner[]; hasFilters: boolean; onAdd: () => void; onEdit: (partner: Partner) => void; onArchive: (partner: Partner) => void }) {
  const plural = kind === "customer" ? "customers" : "suppliers"
  if (partners.length === 0) return <EmptyState title={hasFilters ? `No matching ${plural}` : `No ${plural} yet`} description={hasFilters ? "Try changing or clearing your filters." : `Add your first ${kind} to get started.`} action={!hasFilters ? <Button onClick={onAdd}>Add {kind}</Button> : undefined} />

  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[900px] text-left text-sm">
          <thead className="border-b bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">ICE</th><th className="px-4 py-3">Contact Person</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Phone</th><th className="px-4 py-3">Status</th><th className="px-4 py-3 text-right">Actions</th></tr></thead>
          <tbody className="divide-y">{partners.map((partner) => <tr className="hover:bg-slate-50/60" key={partner.id}><td className="px-4 py-3 font-medium">{partner.name}</td><td className="px-4 py-3 text-muted-foreground">{partner.ice || "—"}</td><td className="px-4 py-3 text-muted-foreground">{partner.contactPerson || "—"}</td><td className="px-4 py-3 text-muted-foreground">{partner.email || "—"}</td><td className="px-4 py-3 text-muted-foreground">{partner.phone || "—"}</td><td className="px-4 py-3"><StatusBadge variant={partner.active ? "success" : "neutral"}>{partner.active ? "Active" : "Archived"}</StatusBadge></td><td className="px-4 py-3"><Actions partner={partner} onArchive={onArchive} onEdit={onEdit} /></td></tr>)}</tbody>
        </table>
      </div>
      <div className="divide-y md:hidden">{partners.map((partner) => <article className="p-4" key={partner.id}><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="truncate font-medium">{partner.name}</p><p className="mt-1 text-xs text-muted-foreground">{partner.ice ? `ICE ${partner.ice}` : "No ICE provided"}</p></div><StatusBadge variant={partner.active ? "success" : "neutral"}>{partner.active ? "Active" : "Archived"}</StatusBadge></div><div className="mt-4 space-y-2 text-sm text-muted-foreground">{partner.contactPerson && <p className="text-foreground">{partner.contactPerson}</p>}{partner.email && <p className="flex items-center gap-2"><Mail className="size-4" />{partner.email}</p>}{partner.phone && <p className="flex items-center gap-2"><Phone className="size-4" />{partner.phone}</p>}{!partner.email && !partner.phone && <p>No contact details provided</p>}</div><div className="mt-3 border-t pt-2"><Actions partner={partner} onArchive={onArchive} onEdit={onEdit} /></div></article>)}</div>
    </>
  )
}
