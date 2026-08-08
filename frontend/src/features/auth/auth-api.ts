import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';
import type {
  AuthTokenResponse,
  ChangePasswordInput,
  CurrentUser,
  LoginInput,
  RegisterInput,
  RegistrationResponse,
  UpdateProfileInput,
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

export async function updateProfile(input: UpdateProfileInput): Promise<CurrentUser> {
  const response = await httpClient.put<ApiResponse<CurrentUser>>('/v1/auth/me', input);
  return response.data.data;
}

export async function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await httpClient.post<ApiResponse<{ avatarUrl: string }>>(
    '/v1/auth/me/avatar',
    formData,
  );
  return response.data.data.avatarUrl;
}

export async function changePassword(input: ChangePasswordInput): Promise<void> {
  await httpClient.post<ApiResponse<null>>('/v1/auth/change-password', input);
}
