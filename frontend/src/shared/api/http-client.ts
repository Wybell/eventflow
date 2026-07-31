import axios from 'axios';
import { useSessionStore } from '../auth/session-store';
import type { ApiError, ApiResponse } from './api-contract';
import { createRequestId } from './request-id';

const REQUEST_ID_HEADER = 'X-Request-Id';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10_000,
});

httpClient.interceptors.request.use((config) => {
  const token = useSessionStore.getState().accessToken;
  config.headers.set(REQUEST_ID_HEADER, createRequestId());
  if (token !== null) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiError(error)),
);

export function toApiError(error: unknown): ApiError {
  if (!axios.isAxiosError(error)) {
    return {
      code: 2000,
      message: '系统繁忙，请稍后重试',
      status: 0,
      requestId: null,
    };
  }

  const response = error.response;
  const payload = response?.data;
  const status = response?.status ?? 0;
  if (isApiResponse(payload)) {
    return {
      code: payload.code,
      message: payload.message,
      status,
      requestId: payload.requestId,
    };
  }

  return {
    code: 2000,
    message: '网络请求失败，请稍后重试',
    status,
    requestId: null,
  };
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiResponse<unknown>>;
  return (
    typeof candidate.code === 'number' &&
    typeof candidate.message === 'string' &&
    typeof candidate.requestId === 'string'
  );
}
