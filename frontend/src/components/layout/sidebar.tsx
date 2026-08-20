import { X } from "lucide-react"
import { NavLink } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { navigationGroups } from "@/components/layout/navigation"
import { cn } from "@/lib/utils"

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      {open && <button aria-label="Close navigation" className="fixed inset-0 z-30 bg-slate-950/45 lg:hidden" onClick={onClose} />}
      <aside className={cn("fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800 bg-slate-950 text-slate-100 transition-transform lg:translate-x-0", open ? "translate-x-0" : "-translate-x-full")}>
        <div className="flex h-16 items-center justify-between border-b border-slate-800 px-5">
          <div className="flex items-center gap-3">
            <div className="grid size-8 place-items-center rounded-md bg-teal-500 font-bold text-slate-950">W</div>
            <div><p className="text-sm font-semibold">WAFA Gestion</p><p className="text-xs text-slate-400">Operations</p></div>
          </div>
          <Button className="text-slate-300 hover:bg-slate-800 hover:text-white lg:hidden" onClick={onClose} size="icon" variant="ghost">
            <X className="size-5" /><span className="sr-only">Close menu</span>
          </Button>
        </div>
        <nav className="flex-1 space-y-5 overflow-y-auto px-3 py-5" aria-label="Main navigation">
          {navigationGroups.map((group, index) => (
            <div key={group.label ?? index}>
              {group.label && <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">{group.label}</p>}
              <div className="space-y-1">
                {group.items.map(({ label, to, icon: Icon }) => (
                  <NavLink
                    className={({ isActive }) => cn("flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-400 transition-colors hover:bg-slate-900 hover:text-white", isActive && "bg-slate-800 text-white")}
                    end={to === "/"}
                    key={to}
                    onClick={onClose}
                    to={to}
                  >
                    <Icon className="size-[18px]" aria-hidden="true" />{label}
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>
      </aside>
    </>
  )
}
