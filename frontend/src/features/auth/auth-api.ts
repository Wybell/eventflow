import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';
import type { AuthTokenResponse, LoginInput } from './auth-types';

export async function login(input: LoginInput): Promise<AuthTokenResponse> {
  const response = await httpClient.post<ApiResponse<AuthTokenResponse>>('/v1/auth/login', input);
  return response.data.data;
}
