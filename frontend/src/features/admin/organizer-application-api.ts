import { httpClient } from '../../shared/api/http-client';
import type { ApiResponse } from '../../shared/api/api-contract';

export interface OrganizerApplication {
  id: number;
  organizationName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote: string | null;
  createTime: string;
  reviewTime: string | null;
}

export async function getOrganizerApplications(): Promise<OrganizerApplication[]> {
  const response = await httpClient.get<ApiResponse<OrganizerApplication[]>>(
    '/v1/admin/organizer-applications',
  );
  return response.data.data;
}

export async function reviewOrganizerApplication(
  id: number,
  action: 'approve' | 'reject',
  reviewNote = '',
): Promise<void> {
  await httpClient.post<ApiResponse<null>>(`/v1/admin/organizer-applications/${id}/${action}`, {
    reviewNote,
  });
}
