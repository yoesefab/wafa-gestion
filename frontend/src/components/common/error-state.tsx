import { CircleAlert, RotateCcw } from "lucide-react"

import { Button } from "@/components/ui/button"

interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export function ErrorState({ title = "Something went wrong", message = "We couldn't load this information.", onRetry }: ErrorStateProps) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center rounded-lg border border-dashed bg-white p-8 text-center" role="alert">
      <div className="grid size-11 place-items-center rounded-full bg-destructive/10 text-destructive">
        <CircleAlert className="size-5" aria-hidden="true" />
      </div>
      <h2 className="mt-4 font-semibold">{title}</h2>
      <p className="mt-1 max-w-md text-sm text-muted-foreground">{message}</p>
      {onRetry && (
        <Button className="mt-5" onClick={onRetry} size="sm" variant="outline">
          <RotateCcw className="size-4" aria-hidden="true" /> Retry
        </Button>
      )}
    </div>
  )
}
