import { useState, type FormEvent } from "react"
import { Navigate, useNavigate } from "react-router-dom"
import { LoaderCircle } from "lucide-react"

import { ApiError } from "@/api/types"
import { LoadingState } from "@/components/common/loading-state"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/features/auth/hooks/use-auth"

export function LoginPage() {
  const { isAuthenticated, isInitializing, authNotice, clearAuthNotice, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isInitializing) return <LoadingState fullScreen label="Restoring your session…" />
  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearAuthNotice()
    setError("")
    setIsSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      navigate("/dashboard", { replace: true })
    } catch (caught) {
      if (caught instanceof ApiError && caught.code === "INVALID_CREDENTIALS") {
        setError("The email or password is incorrect.")
      } else if (caught instanceof ApiError && caught.status === 0) {
        setError("Unable to reach the server. Check your connection and try again.")
      } else {
        setError("Unable to sign in right now. Please try again.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="grid min-h-screen lg:grid-cols-[1fr_1.1fr]">
      <section className="flex items-center justify-center bg-white px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-10 flex items-center gap-3">
            <div className="grid size-11 place-items-center rounded-lg bg-primary text-lg font-bold text-primary-foreground">W</div>
            <div>
              <p className="font-semibold">WAFA Gestion</p>
              <p className="text-sm text-muted-foreground">Business operations</p>
            </div>
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">Welcome back</h1>
          <p className="mt-2 text-sm text-muted-foreground">Sign in to manage your daily operations.</p>

          <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="email">Email</label>
              <Input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="password">Password</label>
              <Input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </div>
            {authNotice === "expired" && !error && (
              <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800" role="status">Your session has expired. Please sign in again.</p>
            )}
            {authNotice === "restore-error" && !error && (
              <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800" role="status">We couldn&apos;t restore your session. Please sign in again.</p>
            )}
            {error && <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">{error}</p>}
            <Button className="w-full" disabled={isSubmitting} type="submit">
              {isSubmitting && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
              {isSubmitting ? "Signing in…" : "Sign in"}
            </Button>
          </form>
        </div>
      </section>
      <section className="hidden bg-primary p-12 text-primary-foreground lg:flex lg:flex-col lg:justify-end">
        <div className="max-w-lg">
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-white/60">Internal workspace</p>
          <p className="mt-4 text-3xl font-medium leading-tight">Catalog, orders, partners, and inventory in one focused workspace.</p>
        </div>
      </section>
    </main>
  )
}
