import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';
import type {
  AuthTokenResponse,
  CurrentUser,
  LoginInput,
  RegisterInput,
  RegistrationResponse,
} from './auth-types';

export async function login(input: LoginInput): Promise<AuthTokenResponse> {
  const response = await httpClient.post<ApiResponse<AuthTokenResponse>>('/v1/auth/login', input);
  return response.data.data;
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await httpClient.get<ApiResponse<CurrentUser>>('/v1/auth/me');
  return response.data.data;
}

export async function register(input: RegisterInput): Promise<RegistrationResponse> {
  const response = await httpClient.post<ApiResponse<RegistrationResponse>>(
    '/v1/auth/register',
    input,
  );
  return response.data.data;
}
