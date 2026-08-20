import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts"

import { EmptyState } from "@/components/common/empty-state"
import type { MonthlySalesPoint, TopProduct } from "@/features/dashboard/types"
import { formatMoney } from "@/lib/money"

const monthFormatter = new Intl.DateTimeFormat("en", { month: "short" })
const compactNumber = new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 })

export function MonthlyRevenueChart({ data }: { data: MonthlySalesPoint[] }) {
  const chartData = data.map((point) => ({ ...point, label: monthFormatter.format(new Date(`${point.month}-01T00:00:00`)) }))
  const hasRevenue = chartData.some((point) => point.totalAmount > 0)
  if (!hasRevenue) return <EmptyState title="No revenue yet" description="Confirmed sales will appear in the monthly revenue chart." />

  return (
    <div className="h-72 w-full" role="img" aria-label="Monthly revenue bar chart">
      <ResponsiveContainer height="100%" width="100%">
        <BarChart data={chartData} margin={{ left: 0, right: 8, top: 12, bottom: 0 }}>
          <CartesianGrid stroke="#e2e8f0" strokeDasharray="3 3" vertical={false} />
          <XAxis axisLine={false} dataKey="label" fontSize={12} tickLine={false} />
          <YAxis axisLine={false} fontSize={12} tickFormatter={(value: number) => compactNumber.format(value)} tickLine={false} width={48} />
          <Tooltip formatter={(value) => [formatMoney(Number(value)), "Revenue"]} labelFormatter={(label) => `${label} revenue`} />
          <Bar dataKey="totalAmount" fill="#0f766e" isAnimationActive={false} maxBarSize={36} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export function TopProductsChart({ data }: { data: TopProduct[] }) {
  if (data.length === 0) return <EmptyState title="No selling data yet" description="Products from confirmed sales will appear here." />
  const chartData = data.map((item) => ({ ...item, label: item.product.name }))

  return (
    <div className="h-72 w-full" role="img" aria-label="Top-selling products bar chart">
      <ResponsiveContainer height="100%" width="100%">
        <BarChart data={chartData} layout="vertical" margin={{ left: 8, right: 20, top: 12, bottom: 0 }}>
          <CartesianGrid horizontal={false} stroke="#e2e8f0" strokeDasharray="3 3" />
          <XAxis allowDecimals={false} axisLine={false} fontSize={12} tickLine={false} type="number" />
          <YAxis axisLine={false} dataKey="label" fontSize={12} tickLine={false} type="category" width={105} />
          <Tooltip formatter={(value) => [`${Number(value)} units`, "Quantity sold"]} />
          <Bar dataKey="quantitySold" fill="#334155" isAnimationActive={false} maxBarSize={26} radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
