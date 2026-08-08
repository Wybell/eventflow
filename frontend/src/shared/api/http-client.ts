import axios from 'axios';
import { useSessionStore } from '../auth/session-store';
import type { ApiError, ApiResponse } from './api-contract';
import { createRequestId } from './request-id';

const REQUEST_ID_HEADER = 'X-Request-Id';
const REFRESH_EXCLUDED_PATHS = [
  '/v1/auth/login',
  '/v1/auth/logout',
  '/v1/auth/refresh',
  '/v1/auth/register',
];

interface AuthTokenResponsePayload {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10_000,
});

let refreshPromise: Promise<string | null> | null = null;

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
  async (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && error.config !== undefined) {
      const retryConfig = error.config as typeof error.config & { _retry?: boolean };
      if (!retryConfig._retry && !isRefreshExcludedPath(retryConfig.url)) {
        retryConfig._retry = true;
        const accessToken = await refreshAccessToken();
        if (accessToken !== null) {
          retryConfig.headers.set('Authorization', `Bearer ${accessToken}`);
          return httpClient.request(retryConfig);
        }
      }
    }
    return Promise.reject(toApiError(error));
  },
);

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = useSessionStore.getState().refreshToken;
  if (refreshToken === null || refreshToken === undefined) {
    return null;
  }
  if (refreshPromise === null) {
    refreshPromise = refreshClient
      .post<ApiResponse<AuthTokenResponsePayload>>('/v1/auth/refresh', { refreshToken })
      .then((response) => {
        const tokens = response.data.data;
        useSessionStore.getState().updateTokens(tokens.accessToken, tokens.refreshToken);
        return tokens.accessToken;
      })
      .catch(() => {
        useSessionStore.getState().clearSession();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function isRefreshExcludedPath(url: string | undefined): boolean {
  if (url === undefined) {
    return false;
  }
  const path = url.split('?')[0];
  return REFRESH_EXCLUDED_PATHS.some((endpoint) => path.endsWith(endpoint));
}

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
