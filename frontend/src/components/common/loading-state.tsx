import { LoaderCircle } from "lucide-react"

import { cn } from "@/lib/utils"

interface LoadingStateProps {
  label?: string
  fullScreen?: boolean
}

export function LoadingState({ label = "Loading…", fullScreen = false }: LoadingStateProps) {
  return (
    <div className={cn("flex items-center justify-center gap-3 text-sm text-muted-foreground", fullScreen ? "min-h-screen" : "min-h-48")} role="status">
      <LoaderCircle className="size-5 animate-spin text-accent" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}
