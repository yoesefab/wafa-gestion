import { ArrowLeft } from "lucide-react"
import { Link } from "react-router-dom"

import { PageHeader } from "@/components/common/page-header"
import { Button } from "@/components/ui/button"
import { PurchaseOrderForm } from "@/features/purchases/components/purchase-order-form"

export function NewPurchaseOrderPage() {
  return <section className="space-y-6"><PageHeader title="New purchase order" description="Create a draft. Inventory changes only when an ordered purchase is received." actions={<Button asChild variant="outline"><Link to="/purchases"><ArrowLeft className="size-4" />Back to purchases</Link></Button>} /><PurchaseOrderForm /></section>
}
