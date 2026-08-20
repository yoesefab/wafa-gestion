import type { LucideIcon } from "lucide-react"

interface PlaceholderPageProps {
  title: string
  description: string
  icon: LucideIcon
}

export function PlaceholderPage({ title, description, icon: Icon }: PlaceholderPageProps) {
  return (
    <section>
      <div className="flex items-center gap-3">
        <div className="grid size-10 place-items-center rounded-lg border bg-white text-muted-foreground shadow-sm">
          <Icon className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
      <div className="mt-8 rounded-lg border border-dashed bg-white px-6 py-16 text-center">
        <p className="font-medium">Ready for feature implementation</p>
        <p className="mt-1 text-sm text-muted-foreground">The route and shared application infrastructure are in place.</p>
      </div>
    </section>
  )
}
