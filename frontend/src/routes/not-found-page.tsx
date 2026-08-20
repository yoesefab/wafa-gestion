import { ArrowLeft } from "lucide-react"
import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"

export function NotFoundPage() {
  return (
    <section className="flex min-h-[65vh] flex-col items-center justify-center text-center">
      <p className="text-sm font-semibold text-accent">404</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">Page not found</h1>
      <p className="mt-3 max-w-md text-sm text-muted-foreground">The page may have moved, or the address may be incorrect.</p>
      <Button asChild className="mt-6" variant="outline"><Link to="/dashboard"><ArrowLeft className="size-4" />Back to dashboard</Link></Button>
    </section>
  )
}
