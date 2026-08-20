import { ApiError } from "@/api/types"

export function purchaseErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return "The operation could not be completed. Please try again."
  if (error.code === "INVALID_STATE_TRANSITION") return error.message || "This action is not allowed for the current order status."
  if (error.code === "INACTIVE_REFERENCE") return "The supplier or one of the products is archived. Update the draft before continuing."
  if (error.code === "VERSION_CONFLICT") return "This purchase order changed since it was loaded. Reload it and try again."
  if (error.code === "DUPLICATE_ORDER_ITEM") return "Each product can appear only once. Update the existing line instead."
  return error.message
}
