export function decimalToScaled(value: string | number, digits: number): bigint {
  const normalized = typeof value === "number" ? value.toFixed(digits) : value.trim()
  if (!/^\d+(\.\d+)?$/.test(normalized)) return 0n
  const [whole, fraction = ""] = normalized.split(".")
  return BigInt(whole) * 10n ** BigInt(digits) + BigInt(fraction.padEnd(digits, "0").slice(0, digits) || "0")
}

export function calculateLine(quantity: string, unitPrice: number, taxRate: string) {
  const parsedQuantity = /^\d+$/.test(quantity) ? BigInt(quantity) : 0n
  const subtotal = parsedQuantity * decimalToScaled(unitPrice, 2)
  const taxHundredths = decimalToScaled(taxRate, 2)
  const tax = (subtotal * taxHundredths + 5_000n) / 10_000n
  return { subtotal, tax, total: subtotal + tax }
}

export function formatCents(value: bigint) {
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD", minimumFractionDigits: 2 }).format(Number(value) / 100)
}

export const formatMoney = (value: number) => new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD", minimumFractionDigits: 2 }).format(value)
