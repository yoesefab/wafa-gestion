import { lazy, Suspense } from "react"
import { createBrowserRouter, Navigate } from "react-router-dom"

import { LoadingState } from "@/components/common/loading-state"
import { AppLayout } from "@/components/layout/app-layout"
import { LoginPage } from "@/features/auth/pages/login-page"
import { NotFoundPage } from "@/routes/not-found-page"
import { ProtectedRoute } from "@/routes/protected-route"
import { RouteErrorPage } from "@/routes/route-error-page"

const ProductsPage = lazy(() => import("@/features/catalog/pages/products-page").then((module) => ({ default: module.ProductsPage })))
const CategoriesPage = lazy(() => import("@/features/catalog/pages/categories-page").then((module) => ({ default: module.CategoriesPage })))
const CustomersPage = lazy(() => import("@/features/customers/pages/customers-page").then((module) => ({ default: module.CustomersPage })))
const SuppliersPage = lazy(() => import("@/features/suppliers/pages/suppliers-page").then((module) => ({ default: module.SuppliersPage })))
const SalesOrdersPage = lazy(() => import("@/features/sales/pages/sales-orders-page").then((module) => ({ default: module.SalesOrdersPage })))
const NewSalesOrderPage = lazy(() => import("@/features/sales/pages/new-sales-order-page").then((module) => ({ default: module.NewSalesOrderPage })))
const SalesOrderDetailPage = lazy(() => import("@/features/sales/pages/sales-order-detail-page").then((module) => ({ default: module.SalesOrderDetailPage })))
const PurchaseOrdersPage = lazy(() => import("@/features/purchases/pages/purchase-orders-page").then((module) => ({ default: module.PurchaseOrdersPage })))
const NewPurchaseOrderPage = lazy(() => import("@/features/purchases/pages/new-purchase-order-page").then((module) => ({ default: module.NewPurchaseOrderPage })))
const PurchaseOrderDetailPage = lazy(() => import("@/features/purchases/pages/purchase-order-detail-page").then((module) => ({ default: module.PurchaseOrderDetailPage })))
const StockMovementsPage = lazy(() => import("@/features/inventory/pages/stock-movements-page").then((module) => ({ default: module.StockMovementsPage })))
const DashboardPage = lazy(() => import("@/features/dashboard/pages/dashboard-page").then((module) => ({ default: module.DashboardPage })))

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage />, errorElement: <RouteErrorPage /> },
  {
    element: <ProtectedRoute />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "dashboard", element: <Suspense fallback={<LoadingState label="Loading dashboard…" />}><DashboardPage /></Suspense> },
          { path: "products", element: <Suspense fallback={<LoadingState label="Loading products…" />}><ProductsPage /></Suspense> },
          { path: "categories", element: <Suspense fallback={<LoadingState label="Loading categories…" />}><CategoriesPage /></Suspense> },
          { path: "sales", element: <Suspense fallback={<LoadingState label="Loading sales orders…" />}><SalesOrdersPage /></Suspense> },
          { path: "sales/new", element: <Suspense fallback={<LoadingState label="Preparing sales order…" />}><NewSalesOrderPage /></Suspense> },
          { path: "sales/:id", element: <Suspense fallback={<LoadingState label="Loading sales order…" />}><SalesOrderDetailPage /></Suspense> },
          { path: "customers", element: <Suspense fallback={<LoadingState label="Loading customers…" />}><CustomersPage /></Suspense> },
          { path: "purchases", element: <Suspense fallback={<LoadingState label="Loading purchase orders…" />}><PurchaseOrdersPage /></Suspense> },
          { path: "purchases/new", element: <Suspense fallback={<LoadingState label="Preparing purchase order…" />}><NewPurchaseOrderPage /></Suspense> },
          { path: "purchases/:id", element: <Suspense fallback={<LoadingState label="Loading purchase order…" />}><PurchaseOrderDetailPage /></Suspense> },
          { path: "suppliers", element: <Suspense fallback={<LoadingState label="Loading suppliers…" />}><SuppliersPage /></Suspense> },
          { path: "inventory", element: <Suspense fallback={<LoadingState label="Loading stock movements…" />}><StockMovementsPage /></Suspense> },
          { path: "*", element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
