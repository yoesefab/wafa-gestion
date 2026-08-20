import { ApiError } from "@/api/types"

export function salesErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return "The operation could not be completed. Please try again."
  if (error.code === "INSUFFICIENT_STOCK") return error.message || "There is not enough stock to confirm this order."
  if (error.code === "INVALID_STATE_TRANSITION") return error.message || "This action is not allowed for the current order status."
  if (error.code === "INACTIVE_REFERENCE") return "The customer or one of the products is archived. Update the draft before continuing."
  if (error.code === "VERSION_CONFLICT") return "This order changed since it was loaded. Reload it and try again."
  return error.message
}
