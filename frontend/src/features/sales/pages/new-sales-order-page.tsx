import { ArrowLeft } from "lucide-react"
import { Link } from "react-router-dom"

import { PageHeader } from "@/components/common/page-header"
import { Button } from "@/components/ui/button"
import { SalesOrderForm } from "@/features/sales/components/sales-order-form"

export function NewSalesOrderPage() {
  return <section className="space-y-6"><PageHeader title="New sales order" description="Create a draft. Stock is checked only when the order is confirmed." actions={<Button asChild variant="outline"><Link to="/sales"><ArrowLeft className="size-4" />Back to sales</Link></Button>} /><SalesOrderForm /></section>
}
