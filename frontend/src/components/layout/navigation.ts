import { Boxes, ContactRound, LayoutDashboard, Package, ShoppingCart, Tags, Truck, Warehouse, type LucideIcon } from "lucide-react"

interface NavigationGroup {
  label: string | null
  items: { label: string; to: string; icon: LucideIcon }[]
}

export const navigationGroups: NavigationGroup[] = [
  {
    label: null,
    items: [{ label: "Dashboard", to: "/dashboard", icon: LayoutDashboard }],
  },
  {
    label: "Catalog",
    items: [
      { label: "Products", to: "/products", icon: Package },
      { label: "Categories", to: "/categories", icon: Tags },
    ],
  },
  {
    label: "Sales",
    items: [
      { label: "Sales Orders", to: "/sales", icon: ShoppingCart },
      { label: "Customers", to: "/customers", icon: ContactRound },
    ],
  },
  {
    label: "Purchasing",
    items: [
      { label: "Purchase Orders", to: "/purchases", icon: Truck },
      { label: "Suppliers", to: "/suppliers", icon: Boxes },
    ],
  },
  {
    label: "Inventory",
    items: [{ label: "Stock Movements", to: "/inventory", icon: Warehouse }],
  },
]

export function getPageTitle(pathname: string) {
  if (pathname.startsWith("/sales")) return "Sales Orders"
  if (pathname.startsWith("/purchases")) return "Purchase Orders"
  return navigationGroups.flatMap((group) => group.items).find((item) => item.to === pathname)?.label ?? "WAFA Gestion"
}
