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
