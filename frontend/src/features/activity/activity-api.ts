import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';
import type {
  Activity,
  ActivityInput,
  ActivitySession,
  ActivitySessionInput,
} from './activity-types';

interface IdResponse {
  id: number;
}

export async function getMyActivities(): Promise<Activity[]> {
  const response = await httpClient.get<ApiResponse<Activity[]>>('/v1/activities/mine');
  return response.data.data;
}

export async function getPublishedActivities(): Promise<Activity[]> {
  const response = await httpClient.get<ApiResponse<Activity[]>>('/v1/activities/public');
  return response.data.data;
}

export async function createActivity(input: ActivityInput): Promise<number> {
  const response = await httpClient.post<ApiResponse<IdResponse>>('/v1/activities', input);
  return response.data.data.id;
}

export async function updateActivity(id: number, input: ActivityInput): Promise<void> {
  await httpClient.put<ApiResponse<null>>(`/v1/activities/${id}`, input);
}

export async function submitActivityForReview(id: number): Promise<void> {
  await httpClient.post<ApiResponse<null>>(`/v1/activities/${id}/submit-review`);
}

export async function getActivitySessions(activityId: number): Promise<ActivitySession[]> {
  const response = await httpClient.get<ApiResponse<ActivitySession[]>>(
    `/v1/activities/${activityId}/sessions`,
  );
  return response.data.data;
}

export async function getPublishedActivitySessions(activityId: number): Promise<ActivitySession[]> {
  const response = await httpClient.get<ApiResponse<ActivitySession[]>>(
    `/v1/activities/${activityId}/sessions/public`,
  );
  return response.data.data;
}

export async function createActivitySession(
  activityId: number,
  input: ActivitySessionInput,
): Promise<number> {
  const response = await httpClient.post<ApiResponse<IdResponse>>(
    `/v1/activities/${activityId}/sessions`,
    input,
  );
  return response.data.data.id;
}
