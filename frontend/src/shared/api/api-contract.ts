export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
}

export interface ApiError {
  code: number;
  message: string;
  status: number;
  requestId: string | null;
}
