import { PackageOpen } from "lucide-react"
import type { ReactNode } from "react"

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <div className="flex min-h-64 flex-col items-center justify-center p-8 text-center">
      <div className="grid size-11 place-items-center rounded-full bg-secondary text-muted-foreground"><PackageOpen className="size-5" /></div>
      <h2 className="mt-4 font-semibold">{title}</h2>
      <p className="mt-1 max-w-md text-sm text-muted-foreground">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
