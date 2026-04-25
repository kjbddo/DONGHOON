export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;     // 0-based
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
