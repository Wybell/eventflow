import type { ApiResponse } from '../../shared/api/api-contract';
import { httpClient } from '../../shared/api/http-client';
import type { ActivityRegistration, RegistrationInput } from './registration-types';

interface IdResponse {
  id: number;
}

export async function registerForActivity(input: RegistrationInput): Promise<number> {
  const response = await httpClient.post<ApiResponse<IdResponse>>('/v1/registrations', input);
  return response.data.data.id;
}

export async function getMyRegistrations(): Promise<ActivityRegistration[]> {
  const response =
    await httpClient.get<ApiResponse<ActivityRegistration[]>>('/v1/registrations/mine');
  return response.data.data;
}

export async function cancelRegistration(id: number): Promise<void> {
  await httpClient.post<ApiResponse<null>>(`/v1/registrations/${id}/cancel`);
}
