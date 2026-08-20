import { LogOut, Menu, UserRound } from "lucide-react"
import { useLocation } from "react-router-dom"

import { getPageTitle } from "@/components/layout/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/features/auth/hooks/use-auth"

export function Navbar({ onMenuClick }: { onMenuClick: () => void }) {
  const { pathname } = useLocation()
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8">
      <div className="flex items-center gap-3">
        <Button className="lg:hidden" onClick={onMenuClick} size="icon" variant="ghost"><Menu className="size-5" /><span className="sr-only">Open menu</span></Button>
        <p className="font-semibold">{getPageTitle(pathname)}</p>
      </div>
      <div className="flex items-center gap-2">
        <div className="hidden items-center gap-2 pr-2 sm:flex">
          <div className="grid size-8 place-items-center rounded-full bg-secondary"><UserRound className="size-4 text-muted-foreground" /></div>
          <div className="max-w-40"><p className="truncate text-sm font-medium">{user ? `${user.firstName} ${user.lastName}` : ""}</p><p className="truncate text-xs text-muted-foreground">{user?.email}</p></div>
        </div>
        <Button onClick={logout} size="icon" title="Sign out" variant="ghost"><LogOut className="size-4" /><span className="sr-only">Sign out</span></Button>
      </div>
    </header>
  )
}
