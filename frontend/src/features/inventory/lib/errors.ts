import { ApiError } from "@/api/types"

export function inventoryErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return "The stock adjustment could not be completed. Check your connection and try again."
  if (error.code === "INSUFFICIENT_STOCK") return error.message || "There is not enough stock for this adjustment."
  if (error.code === "INACTIVE_REFERENCE") return "This product is archived and cannot be adjusted."
  if (error.code === "STOCK_LIMIT_EXCEEDED") return error.message || "This adjustment exceeds the supported stock limit."
  return error.message
}
