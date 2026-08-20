import { cn } from "@/lib/utils"

export function LoadingSkeleton({ rows = 6, className }: { rows?: number; className?: string }) {
  return (
    <div className={cn("animate-pulse space-y-3 p-5", className)} aria-label="Loading" role="status">
      {Array.from({ length: rows }, (_, index) => <div className="h-11 rounded-md bg-slate-100" key={index} />)}
    </div>
  )
}
