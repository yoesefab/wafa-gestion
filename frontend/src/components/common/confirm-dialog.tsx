import { LoaderCircle } from "lucide-react"

import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog"
import { buttonVariants } from "@/components/ui/button-variants"

interface ConfirmDialogProps {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  pending?: boolean
  variant?: "default" | "destructive"
  onConfirm: () => void
  onOpenChange: (open: boolean) => void
}

export function ConfirmDialog({ open, title, description, confirmLabel, pending, variant = "destructive", onConfirm, onOpenChange }: ConfirmDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader><AlertDialogTitle className="text-lg font-semibold">{title}</AlertDialogTitle><AlertDialogDescription className="text-sm text-muted-foreground">{description}</AlertDialogDescription></AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel className={buttonVariants({ variant: "outline" })} disabled={pending}>Cancel</AlertDialogCancel>
          <AlertDialogAction className={buttonVariants({ variant })} disabled={pending} onClick={(event) => { event.preventDefault(); onConfirm() }}>
            {pending && <LoaderCircle className="size-4 animate-spin" />}{pending ? "Working…" : confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
