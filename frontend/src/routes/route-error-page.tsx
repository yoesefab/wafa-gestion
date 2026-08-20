import { useRouteError } from "react-router-dom"

import { ErrorState } from "@/components/common/error-state"

export function RouteErrorPage() {
  const error = useRouteError()
  const message = error instanceof Error ? error.message : "The page could not be displayed."
  return <main className="grid min-h-screen place-items-center p-6"><div className="w-full max-w-xl"><ErrorState message={message} onRetry={() => window.location.reload()} /></div></main>
}
