import { useState, type FormEvent } from "react"
import { LoaderCircle } from "lucide-react"

import { ApiError } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useCreateCategory, useUpdateCategory } from "@/features/catalog/hooks/use-categories"
import type { Category } from "@/features/catalog/types"
import { notifications } from "@/lib/notifications"

export function CategoryFormDialog({ category, onClose }: { category: Category | null; onClose: () => void }) {
  const [name, setName] = useState(category?.name ?? "")
  const [description, setDescription] = useState(category?.description ?? "")
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState("")
  const createMutation = useCreateCategory()
  const updateMutation = useUpdateCategory()
  const mutation = category ? updateMutation : createMutation

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFieldErrors({})
    setFormError("")
    const request = { name: name.trim(), description: description.trim() || null }
    try {
      if (category) await updateMutation.mutateAsync({ id: category.id, request: { ...request, version: category.version } })
      else await createMutation.mutateAsync(request)
      notifications.success(category ? "Category updated" : "Category created", `${request.name} was saved successfully.`)
      onClose()
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(Object.fromEntries(error.fieldErrors.map((item) => [item.field, item.message])))
        setFormError(error.code === "VERSION_CONFLICT" ? "This category changed since you opened it. Close the form and try again." : error.message)
      } else setFormError("The category could not be saved. Please try again.")
    }
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose() }}>
      <DialogContent>
        <DialogHeader><DialogTitle className="text-xl font-semibold">{category ? "Edit category" : "Add category"}</DialogTitle><DialogDescription className="text-sm text-muted-foreground">Categories organize products in the catalog.</DialogDescription></DialogHeader>
        <form onSubmit={submit}>
          <div className="mt-6 space-y-4">
            <div className="space-y-2"><label className="text-sm font-medium" htmlFor="category-name">Name</label><Input id="category-name" maxLength={120} required value={name} onChange={(event) => { setName(event.target.value); setFieldErrors((current) => ({ ...current, name: "" })) }} />{fieldErrors.name && <p className="text-xs text-destructive">{fieldErrors.name}</p>}</div>
            <div className="space-y-2"><label className="text-sm font-medium" htmlFor="category-description">Description</label><textarea className="min-h-28 w-full resize-y rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2" id="category-description" maxLength={500} value={description} onChange={(event) => { setDescription(event.target.value); setFieldErrors((current) => ({ ...current, description: "" })) }} />{fieldErrors.description && <p className="text-xs text-destructive">{fieldErrors.description}</p>}</div>
          </div>
          {formError && <p className="mt-4 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">{formError}</p>}
          <DialogFooter><DialogClose asChild><Button disabled={mutation.isPending} type="button" variant="outline">Cancel</Button></DialogClose><Button disabled={mutation.isPending} type="submit">{mutation.isPending && <LoaderCircle className="size-4 animate-spin" />}{mutation.isPending ? "Saving…" : "Save category"}</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
