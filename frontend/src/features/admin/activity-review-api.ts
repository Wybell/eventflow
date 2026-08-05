import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';
import type { Activity } from '../activity/activity-types';

export async function getPendingActivityReviews(): Promise<Activity[]> {
  const response = await httpClient.get<ApiResponse<Activity[]>>(
    '/v1/admin/activities/pending-review',
  );
  return response.data.data;
}

export async function reviewActivity(
  id: number,
  action: 'approve' | 'reject',
  reviewNote = '',
): Promise<void> {
  await httpClient.post<ApiResponse<null>>(`/v1/admin/activities/${id}/${action}`, { reviewNote });
}
