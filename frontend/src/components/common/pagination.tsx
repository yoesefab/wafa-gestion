import { ChevronLeft, ChevronRight } from "lucide-react"

import { Button } from "@/components/ui/button"

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, pageSize, onPageChange }: PaginationProps) {
  if (totalPages === 0) return null
  const start = page * pageSize + 1
  const end = Math.min((page + 1) * pageSize, totalElements)
  return (
    <div className="flex flex-col gap-3 border-t px-4 py-3 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
      <p>Showing {start}–{end} of {totalElements}</p>
      <div className="flex flex-wrap items-center gap-2">
        <Button disabled={page === 0} onClick={() => onPageChange(page - 1)} size="sm" variant="outline"><ChevronLeft className="size-4" />Previous</Button>
        <span className="px-1">Page {page + 1} of {totalPages}</span>
        <Button disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)} size="sm" variant="outline">Next<ChevronRight className="size-4" /></Button>
      </div>
    </div>
  )
}
