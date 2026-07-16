export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string
}

export interface ErrorResponse {
  success: false
  error: {
    code: string
    message: string
  }
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
