export interface ApiEnvelope<T> {
  data: T
}

export interface PageMetadata {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PagedResponse<T> {
  data: T[]
  page: PageMetadata
}

export interface ApiFieldError {
  field: string
  code: string
  message: string
}

export interface ApiProblem {
  type?: string
  title: string
  status: number
  code?: string
  detail: string
  instance?: string
  timestamp?: string
  traceId?: string
  fieldErrors: ApiFieldError[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors: ApiFieldError[]
  readonly traceId?: string

  constructor(problem: ApiProblem) {
    super(problem.detail || problem.title)
    this.name = "ApiError"
    this.status = problem.status
    this.code = problem.code
    this.fieldErrors = problem.fieldErrors ?? []
    this.traceId = problem.traceId
  }
}
