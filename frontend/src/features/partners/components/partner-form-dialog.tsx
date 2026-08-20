import { useState, type FormEvent } from "react"
import { LoaderCircle } from "lucide-react"

import { ApiError } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useCreatePartner, useUpdatePartner } from "@/features/partners/hooks/use-partners"
import type { Partner, PartnerKind, PartnerResource, PartnerWriteRequest } from "@/features/partners/types"
import { notifications } from "@/lib/notifications"

type FormField = keyof PartnerWriteRequest

export function PartnerFormDialog({ kind, resource, partner, onClose }: { kind: PartnerKind; resource: PartnerResource; partner: Partner | null; onClose: () => void }) {
  const [values, setValues] = useState<PartnerWriteRequest>({
    name: partner?.name ?? "", ice: partner?.ice ?? "", contactPerson: partner?.contactPerson ?? "", email: partner?.email ?? "", phone: partner?.phone ?? "", address: partner?.address ?? "",
  })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState("")
  const createMutation = useCreatePartner(resource)
  const updateMutation = useUpdatePartner(resource)
  const mutation = partner ? updateMutation : createMutation
  const label = kind === "customer" ? "customer" : "supplier"

  function update(field: FormField, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: "" }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFieldErrors({})
    setFormError("")
    const optional = (value: string | null) => value?.trim() || null
    const request: PartnerWriteRequest = { name: values.name.trim(), ice: optional(values.ice), contactPerson: optional(values.contactPerson), email: optional(values.email), phone: optional(values.phone), address: optional(values.address) }
    try {
      if (partner) await updateMutation.mutateAsync({ id: partner.id, request: { ...request, version: partner.version } })
      else await createMutation.mutateAsync(request)
      notifications.success(partner ? `${label[0].toUpperCase() + label.slice(1)} updated` : `${label[0].toUpperCase() + label.slice(1)} created`, `${request.name} was saved successfully.`)
      onClose()
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(Object.fromEntries(error.fieldErrors.map((item) => [item.field, item.message])))
        setFormError(error.code === "VERSION_CONFLICT" ? `This ${label} changed since you opened it. Close the form and try again.` : error.message)
      } else setFormError(`The ${label} could not be saved. Please try again.`)
    }
  }

  function input(field: FormField, title: string, maxLength: number, type = "text") {
    return <div className="space-y-2"><label className="text-sm font-medium" htmlFor={`${label}-${field}`}>{title}</label><Input id={`${label}-${field}`} maxLength={maxLength} required={field === "name"} type={type} value={values[field] ?? ""} onChange={(event) => update(field, event.target.value)} />{fieldErrors[field] && <p className="text-xs text-destructive">{fieldErrors[field]}</p>}</div>
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose() }}>
      <DialogContent>
        <DialogHeader><DialogTitle className="text-xl font-semibold">{partner ? `Edit ${label}` : `Add ${label}`}</DialogTitle><DialogDescription className="text-sm text-muted-foreground">Keep contact and company details current for operational records.</DialogDescription></DialogHeader>
        <form onSubmit={submit}>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">{input("name", "Name or company name", 180)}</div>
            {input("ice", "ICE / tax identifier", 30)}
            {input("contactPerson", "Contact person", 180)}
            {input("email", "Email", 254, "email")}
            {input("phone", "Phone", 30, "tel")}
            <div className="space-y-2 sm:col-span-2"><label className="text-sm font-medium" htmlFor={`${label}-address`}>Address</label><textarea className="min-h-24 w-full resize-y rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2" id={`${label}-address`} maxLength={500} value={values.address ?? ""} onChange={(event) => update("address", event.target.value)} />{fieldErrors.address && <p className="text-xs text-destructive">{fieldErrors.address}</p>}</div>
          </div>
          {formError && <p className="mt-4 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">{formError}</p>}
          <DialogFooter><DialogClose asChild><Button disabled={mutation.isPending} type="button" variant="outline">Cancel</Button></DialogClose><Button disabled={mutation.isPending} type="submit">{mutation.isPending && <LoaderCircle className="size-4 animate-spin" />}{mutation.isPending ? "Saving…" : `Save ${label}`}</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
